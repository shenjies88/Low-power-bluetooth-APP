package example.com.clientapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.githang.statusbar.StatusBarCompat;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import example.com.clientapp.Bluetooth.BluetoothContral;
import example.com.clientapp.Utils.ActivityCollector;
import example.com.clientapp.Utils.CacheUtils;
import example.com.clientapp.Utils.LogUtil;

public class CentralActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.ll_titlebg)
    LinearLayout llTitlebg;
    @BindView(R.id.bt_openblue)
    Button btOpenblue;
    @BindView(R.id.bt_scan)
    Button btScan;
    @BindView(R.id.tv_rssi)
    TextView tvRssi;
    @BindView(R.id.tv_distance)
    TextView tvDistance;
    @BindView(R.id.ll_basicequipmentinformation)
    LinearLayout llBasicequipmentinformation;
    @BindView(R.id.tv_lockstat)
    TextView tvLockstat;
    @BindView(R.id.bt_test)
    Button btTest;
    @BindView(R.id.ll_stat)
    LinearLayout llStat;
    @BindView(R.id.tv_connectstat)
    TextView tvConnectstat;
    @BindView(R.id.bt_setsername)
    Button btSetsername;
    @BindView(R.id.tx_username)
    TextView txUsername;
    @BindView(R.id.bt_setpassword)
    Button btSetpassword;
    @BindView(R.id.bt_setlocalpassword)
    Button btSetlocalpassword;
    @BindView(R.id.bt_showpassword)
    Button btShowpassword;
    @BindView(R.id.tv_showpassword)
    TextView tvShowpassword;

    private Vibrator vibrator;

    private long[] patter = {200, 200, 200, 200};

    private String recognition = "f";

    private String username = null;

    private String password = null;

    private  String showPassword = null;

    private boolean isShowPassword = false;

    String color_red = "#FF4444";
    String color_green = "#669900";
    String color_blue = "#0099CC";

    //监听状态广播
    private StateReceiver stateReceiver = new StateReceiver();

    private BluetoothContral mBluetoothContral;

    //Handler对象
    private Handler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarCompat.setStatusBarColor(this, Color.parseColor("#000000"), false);
        setContentView(R.layout.activity_central);
        ButterKnife.bind(this);

        LogUtil.e("启动Central");

        vibrator = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);

        //初始化
        mBluetoothContral = new BluetoothContral(this);
        if (mBluetoothContral.IsOpen()) {
            btOpenblue.setText("关闭蓝牙");
            mBluetoothContral.StartScan();
            btScan.setText("停止搜索");
        }

        username = CacheUtils.getString(CentralActivity.this, "username");
        if ("".equals(username) || username == null) {
            txUsername.setText("默认用户名");
        } else {
            txUsername.setText("用户名:" + username);
        }
        password = CacheUtils.getString(CentralActivity.this, "password");
        if ("".equals(password) || password == null) {
            password = "111";
        }

        showPassword = password;

        mBluetoothContral.SetPassword(password.getBytes());


        mBluetoothContral.SetUsername(username);

        mBluetoothContral.RegesteHandler(mHandler);

        tvTitle.setText("用户");
        llTitlebg.setBackgroundColor(Color.parseColor("#000000"));

        InitListner();
        BroadcastIni();
    }

    //监听器
    private void InitListner() {
        btOpenblue.setOnClickListener(this);
        btScan.setOnClickListener(this);
        btTest.setOnClickListener(this);
        btSetsername.setOnClickListener(this);
        btSetpassword.setOnClickListener(this);
        btSetlocalpassword.setOnClickListener(this);
        btShowpassword.setOnClickListener(this);
    }

    //广播初始化
    private void BroadcastIni() {
        //注册广播状态广播
        IntentFilter statefilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(stateReceiver, statefilter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_showpassword:{
                if (isShowPassword){
                    btShowpassword.setText("显示密码");
                    tvShowpassword.setVisibility(View.GONE);
                    isShowPassword = false;
                }else {
                    btShowpassword.setText("隐藏密码");
                    tvShowpassword.setText("密码:"+showPassword);
                    tvShowpassword.setVisibility(View.VISIBLE);
                    isShowPassword = true;
                }
            }
            break;

            case R.id.bt_setpassword: {
                if ("f".equals(mBluetoothContral.recognition)) {
                    ShowText("未识别，无法设置密码");
                    vibrator.vibrate(1000);
                } else {
                    showSetPasswordDailog();
                }

            }
            break;

            case R.id.bt_setlocalpassword: {
                showSetLocalPasswordDailog();
            }
            break;

            case R.id.bt_setsername: {

                showSetUsernameDailog();


            }
            break;

            case R.id.bt_openblue: {
                if (mBluetoothContral.IsOpen()) {
                    mBluetoothContral.CloseBlue();
                } else {
                    mBluetoothContral.OpenBlue();
                }
            }
            break;

            case R.id.bt_scan: {
                if (mBluetoothContral.IsOpen()) {
                    if (btScan.getText().toString().equals("停止搜索")) {
                        mBluetoothContral.StopScan();
                        mBluetoothContral.CIsConnect = true;
                        Toast.makeText(CentralActivity.this, "停止搜索", Toast.LENGTH_SHORT).show();
                        btScan.setText("开始搜索");
                    } else {
                        mBluetoothContral.StartScan();
                        llBasicequipmentinformation.setVisibility(View.GONE);
                        btScan.setText("停止搜索");
                    }

                } else {
                    Toast.makeText(this, "请先打开蓝牙", Toast.LENGTH_SHORT).show();
                }
            }
            break;


        }
    }

    //设置本地密码
    private void showSetLocalPasswordDailog() {
        LogUtil.e("设置本地新密码");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View view = View.inflate(this, R.layout.dailog_setlocalpassword, null);
        // dialog.setView(view);// 将自定义的布局文件设置给dialog
        dialog.setView(view, 0, 0, 0, 0);

        final EditText etpassword = view.findViewById(R.id.et_password);
        Button btqueding = view.findViewById(R.id.bt_queding);
        Button btquxiao = view.findViewById(R.id.bt_quexiao);

        btqueding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPassword = etpassword.getText().toString();
                tvShowpassword.setText("密码:"+showPassword);
                CacheUtils.putString(CentralActivity.this, "password", showPassword);
                mBluetoothContral.SetPassword(showPassword.getBytes());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ShowText("密码改变");
                    }
                });
                vibrator.vibrate(1000);
                dialog.dismiss();
            }
        });

        btquxiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        dialog.show();
    }

    //设置用户名弹框
    private void showSetUsernameDailog() {
        LogUtil.e("设置用户名");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();

        View view = View.inflate(this, R.layout.dailog_setusername, null);
        // dialog.setView(view);// 将自定义的布局文件设置给dialog
        dialog.setView(view, 0, 0, 0, 0);
        final EditText et_add_money = view.findViewById(R.id.et_addmoney);
        Button bt_add_money = view.findViewById(R.id.btn_add_money);
        Button bt_money_out = view.findViewById(R.id.btn_money_out);

        bt_add_money.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = et_add_money.getText().toString();
                CacheUtils.putString(CentralActivity.this, "username", username);
                txUsername.setText("用户名:" + username);
                mBluetoothContral.SetUsername(username);
                dialog.dismiss();
            }
        });

        bt_money_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    //设置远端密码
    private void showSetPasswordDailog() {
        LogUtil.e("设置远端新密码");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View view = View.inflate(this, R.layout.dailog_setpassword, null);
        // dialog.setView(view);// 将自定义的布局文件设置给dialog
        dialog.setView(view, 0, 0, 0, 0);

        final EditText repassword = view.findViewById(R.id.et_repassword);
        final EditText newpassword = view.findViewById(R.id.et_newpassword);
        Button btqueding = view.findViewById(R.id.bt_queding);
        Button btquxiao = view.findViewById(R.id.bt_quexiao);

        btqueding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldpassword = repassword.getText().toString();
                LogUtil.e("输入旧密码:"+repassword.getText().toString());
                LogUtil.e("输入新密码:"+newpassword.getText().toString());
                LogUtil.e("password:"+password);

                if (oldpassword.equals(password)) {
                    password = newpassword.getText().toString();
                    showPassword = password;
                    tvShowpassword.setText("密码:"+password);
                    CacheUtils.putString(CentralActivity.this, "password", password);
                    mBluetoothContral.SetPassword(password.getBytes());
                    mBluetoothContral.TKey();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ShowText("密码改变");
                        }
                    });
                    vibrator.vibrate(1000);
                    dialog.dismiss();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ShowText("原密码输入错误");
                        }
                    });
                    vibrator.vibrate(1000);
                }
            }
        });

        btquxiao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });


        dialog.show();

    }

    //状态广播接收器
    class StateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 1);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    //mBluetoothContral.StopScan();
                    btOpenblue.setText("打开蓝牙");
                    LogUtil.e("蓝牙已经关闭");
                    break;
                case BluetoothAdapter.STATE_ON:
                    mBluetoothContral.StartScan();
                    btOpenblue.setText("关闭蓝牙");
                    btScan.setText("停止搜索");
                    LogUtil.e("蓝牙已经打开");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    LogUtil.e("正在打开");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    LogUtil.e("正在关闭");
                    break;
                default:
                    LogUtil.e("未知状态");
            }
        }
    }

    //Handler防止泄漏
    private static class MyHandler extends Handler {
        private final WeakReference<CentralActivity> mActivity;

        public MyHandler(CentralActivity activity) {
            mActivity = new WeakReference<CentralActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CentralActivity activity = mActivity.get();
            if (activity == null) {
                super.handleMessage(msg);
                return;
            }
            switch (msg.what) {

                case BluetoothContral.MSG_THREAD: {
                    LogUtil.e("收到消息了 MSG_THREAD");
                    activity.tvConnectstat.setText("状态：未连接");
                    activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_red));
                    activity.llBasicequipmentinformation.setVisibility(View.GONE);
                    activity.llStat.setVisibility(View.GONE);
                    activity.vibrator.vibrate(1000);
                }
                break;

                case BluetoothContral.MSG_SCAN_DEVICE: {
                    activity.vibrator.cancel();
                    LogUtil.e("收到消息了 MSG_SCANDEVICE");
                    String deviceName = msg.getData().getString(BluetoothContral.KEY_DEVICE_NAME);

                    int rssi = msg.getData().getInt(BluetoothContral.KEY_RSSI);

                    double dis = activity.mBluetoothContral.CalcDistByRSSI(rssi);
                    DecimalFormat decimalFormat = new DecimalFormat("#.00");

                    //实际距离保留小数点后两位
                    String disitance = decimalFormat.format(dis);


                    activity.tvRssi.setText("信号强度：" + rssi);
                    if (Float.parseFloat(disitance) > 5.0) {
                        activity.mBluetoothContral.CDisConnect();
                    } else {

                        if (Float.parseFloat(disitance) < 1.0) {
                            activity.tvDistance.setText("距离:0" + disitance + "米");
                        } else {
                            activity.tvDistance.setText("距离:" + disitance + "米");
                        }
                        activity.llBasicequipmentinformation.setVisibility(View.VISIBLE);


                    }


                }
                break;

                case BluetoothContral.MSG_DISTANT: {
                    LogUtil.e("收到消息了 MSG_DISTANT");

                    int rssi = msg.getData().getInt(BluetoothContral.KEY_RSSI);

                    double dis = activity.mBluetoothContral.CalcDistByRSSI(rssi);
                    DecimalFormat decimalFormat = new DecimalFormat("#.00");

                    //实际距离保留小数点后两位
                    String disitance = decimalFormat.format(dis);

                    activity.tvConnectstat.setText("状态：正在搜索");
                    activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_blue));

                    activity.tvRssi.setText("信号强度：" + rssi);
                    if (Float.parseFloat(disitance) < 1.0) {
                        activity.tvDistance.setText("距离:0" + disitance + "米");
                    } else {
                        activity.tvDistance.setText("距离:" + disitance + "米");
                    }
                    activity.llBasicequipmentinformation.setVisibility(View.VISIBLE);
                    activity.vibrator.vibrate(activity.patter, -1);
                }
                break;

                case BluetoothContral.MSG_DISCONNECT: {
                    LogUtil.e("收到消息了 MSG_DISCONNECT");
                    activity.tvLockstat.setText("锁头状态:ff");
                    activity.tvConnectstat.setText("状态：未连接");
                    activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_red));
                    activity.llBasicequipmentinformation.setVisibility(View.GONE);
                    activity.llStat.setVisibility(View.GONE);
                    activity.Vibrate(1000);
                }
                break;

                case BluetoothContral.MSG_RECOGNITION: {
                    LogUtil.e("收到消息了 MSG_RECOGNITION");
                    activity.recognition = (String) msg.obj;
                    if ("t".equals(activity.recognition)) {
                        activity.tvConnectstat.setText("状态：已经连接");
                        activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_green));
                        activity.llStat.setVisibility(View.VISIBLE);
                    } else {
                        activity.tvConnectstat.setText("状态：识别失败");
                        activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_red));
                        activity.llStat.setVisibility(View.GONE);
                    }
                    activity.vibrator.vibrate(1000);
                }
                break;

                case BluetoothContral.MSG_LOCK_STAT: {
                    LogUtil.e("收到消息了 MSG_LOCKSTAT");
                    String s = (String) msg.obj;
                    if (s.equals("t")) {
                        activity.tvLockstat.setText("锁头状态:打开");
                        activity.Vibrate(500);

                    } else if (s.equals("f")) {
                        activity.tvLockstat.setText("锁头状态:关闭");
                        activity.Vibrate(500);

                    }
                }
                break;


            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(stateReceiver);
        mBluetoothContral.StopScan();
        mBluetoothContral.CDisConnect();
        mBluetoothContral.CloseBlue();
        mBluetoothContral = null;
        mHandler = null;
        ActivityCollector.finishAll();
        Process.killProcess(Process.myPid());
    }

    //重写onKeyUp(),实现连续两次点击方法可推出当前应用
    private boolean flag = true;
    public static final int WHAT_RESET_BACK = 1;
    private Handler handler = new Handler() {


        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case WHAT_RESET_BACK:
                    flag = true;//复原
                    break;
            }
        }
    };

    //震动
    private void Vibrate(long time) {
        vibrator.vibrate(time);
    }

    //Toast
    public void ShowText(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && flag) {
            Toast.makeText(CentralActivity.this, "如果再点击一次，退出当前应用", Toast.LENGTH_SHORT).show();
            flag = false;
            //发送延迟消息
            handler.sendEmptyMessageDelayed(WHAT_RESET_BACK, 2000);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
