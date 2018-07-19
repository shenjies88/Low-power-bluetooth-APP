package example.com.clientapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.githang.statusbar.StatusBarCompat;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import example.com.clientapp.Bluetooth.BluetoothContral;
import example.com.clientapp.Utils.ActivityCollector;
import example.com.clientapp.Utils.CacheUtils;
import example.com.clientapp.Utils.LogUtil;

public class PeripheryActivity extends BaseActivity implements View.OnClickListener {

    @BindView(R.id.bt_openblue)
    Button btOpenblue;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.ll_titlebg)
    LinearLayout llTitlebg;
    @BindView(R.id.tv_devicename)
    TextView tvDevicename;
    @BindView(R.id.ll_connectdevice)
    LinearLayout llConnectdevice;
    @BindView(R.id.tv_lockstat)
    TextView tvLockstat;

    @BindView(R.id.bt_openlock)
    Button btOpenlock;
    @BindView(R.id.iv_lock)
    ImageView ivLock;
    @BindView(R.id.iv_locklight)
    ImageView ivLocklight;
    @BindView(R.id.tv_connectstat)
    TextView tvConnectstat;
    @BindView(R.id.bt_showpassword)
    Button btShowpassword;
    @BindView(R.id.tv_showpassword)
    TextView tvShowpassword;

    private String lockStat = "f";

    private String password = null;

    private boolean isShowPassword = false;

    private Vibrator vibrator;

    String color_red = "#FF4444";
    String color_green = "#669900";

    //监听状态广播
    private StateReceiver stateReceiver = new StateReceiver();

    private BluetoothContral mBluetoothContral;

    private BluetoothDevice CurrentDevice;

    Handler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StatusBarCompat.setStatusBarColor(this, Color.parseColor("#000000"), false);
        setContentView(R.layout.activity_periphery);
        ButterKnife.bind(this);
        LogUtil.e("启动Periphery");

        vibrator = (Vibrator) this.getSystemService(this.VIBRATOR_SERVICE);

        //初始化
        mBluetoothContral = new BluetoothContral(this);
        //获取密码
        password = mBluetoothContral.getScert();
        LogUtil.e("字节数组转字符串密码:"+password);


        if (mBluetoothContral.IsOpen()) {
            btOpenblue.setText("关闭蓝牙");
            mBluetoothContral.InitGATTServer();
        }

        mBluetoothContral.RegesteHandler(mHandler);

        llTitlebg.setBackgroundColor(Color.parseColor("#000000"));
        tvTitle.setText("蓝牙锁");

        initLock();
        InitListner();
        BroadcastIni();

    }

    //锁头初始化
    private void initLock() {
        /*if (CacheUtils.getString(this, BluetoothContral.KEY_LOCK_STAT).equals("")) {
            CacheUtils.putString(this, BluetoothContral.KEY_LOCK_STAT, "f");
            LogUtil.e("第一次锁头进行缓存");
        } else {
            lockStat = CacheUtils.getString(this, BluetoothContral.KEY_LOCK_STAT);
            LogUtil.e("取出锁头状态");
        }*/


        switch (lockStat) {
            case "t":
                tvLockstat.setText("锁头状态：打开");
                btOpenlock.setText("关锁");
                ivLock.setImageResource(R.drawable.open_lock_img);
                ivLocklight.setImageResource(R.drawable.light_green);
                break;
            case "f":
                tvLockstat.setText("锁头状态：关闭");
                btOpenlock.setText("开锁");
                ivLock.setImageResource(R.drawable.close_lock_img);
                ivLocklight.setImageResource(R.drawable.light_red);
                break;
        }

        mBluetoothContral.SetLockstat(lockStat);

    }

    //监听器
    private void InitListner() {
        btOpenblue.setOnClickListener(this);
        btOpenlock.setOnClickListener(this);
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
                    tvShowpassword.setText("密码:"+password);
                    tvShowpassword.setVisibility(View.VISIBLE);
                    isShowPassword = true;
                }
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

            case R.id.bt_openlock: {

                if ("f".equals(mBluetoothContral.recognition) || !mBluetoothContral.getIsConnect()) {
                    ShowText("用户验证错误或未连接，无法操作");
                    vibrator.vibrate(200);
                } else {
                    if (lockStat.equals("t")) {
                        lockStat = "f";
                        Vibrate(500);
                        ivLock.setImageResource(R.drawable.close_lock_img);
                        ivLocklight.setImageResource(R.drawable.light_red);
                        tvLockstat.setText("锁头状态：关闭");
                        ShowText("关锁");
                        btOpenlock.setText("开锁");
                        CacheUtils.putString(this, BluetoothContral.KEY_LOCK_STAT, lockStat);
                        mBluetoothContral.SetLockstat(lockStat);
                        mBluetoothContral.UpLockstat(lockStat);
                    } else if (lockStat.equals("f")) {
                        lockStat = "t";
                        Vibrate(500);
                        ivLock.setImageResource(R.drawable.open_lock_img);
                        ivLocklight.setImageResource(R.drawable.light_green);
                        tvLockstat.setText("锁头状态：打开");
                        ShowText("开锁");
                        btOpenlock.setText("关锁");
                        CacheUtils.putString(this, BluetoothContral.KEY_LOCK_STAT, lockStat);
                        mBluetoothContral.SetLockstat(lockStat);
                        mBluetoothContral.UpLockstat(lockStat);
                    }
                }
            }
            break;
        }

    }

    //状态广播接收器
    class StateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 1);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    btOpenblue.setText("打开蓝牙");
                    mBluetoothContral.StopAdvertise();
                    llConnectdevice.setVisibility(View.GONE);
                    LogUtil.e("蓝牙已经关闭");
                    break;
                case BluetoothAdapter.STATE_ON:
                    mBluetoothContral.InitGATTServer();
                    btOpenblue.setText("关闭蓝牙");
                    LogUtil.e("蓝牙已经打开");
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    LogUtil.e("正在打开");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    ShowText("正在关闭");
                    LogUtil.e("正在关闭");
                    break;
                default:
                    LogUtil.e("未知状态");
            }
        }
    }

    //Handler防止泄漏

    private static class MyHandler extends Handler {
        private final WeakReference<PeripheryActivity> mActivity;

        public MyHandler(PeripheryActivity activity) {
            mActivity = new WeakReference<PeripheryActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PeripheryActivity activity = mActivity.get();
            if (activity == null) {
                super.handleMessage(msg);
                return;
            }
            switch (msg.what) {

                case BluetoothContral.MSG_TKEY:{
                    LogUtil.e("收到消息了 MSG_TKEY");
                    String s = (String) msg.obj;
                    activity.password = s;
                    activity.tvShowpassword.setText("密码:"+s);
                    activity.vibrator.vibrate(1000);
                }
                break;

                case BluetoothContral.MSG_RECOGNITION: {
                    LogUtil.e("收到消息了 MSG_RECOGNITION");
                    String recognition = (String) msg.obj;
                    if ("t".equals(recognition)) {
                        activity.tvConnectstat.setText("状态：已连接");
                        activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_green));
                    } else {
                        activity.tvConnectstat.setText("状态：识别失败");
                        activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_red));
                        activity.llConnectdevice.setVisibility(View.GONE);
                        activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_red));
                        activity.btOpenlock.setText("开锁");
                        activity.ivLock.setImageResource(R.drawable.close_lock_img);
                        activity.ivLocklight.setImageResource(R.drawable.light_red);
                        activity.tvLockstat.setText("锁头状态：关闭");
                        activity.lockStat = "f";
                        activity.mBluetoothContral.SetLockstat(activity.lockStat);
                        CacheUtils.putString(activity, BluetoothContral.KEY_LOCK_STAT, activity.lockStat);
                    }
                    activity.Vibrate(1000);
                }
                break;

                case BluetoothContral.MSG_USERNAME: {
                    LogUtil.e("收到消息了 MSG_USERNAME");
                    String username = (String) msg.obj;
                    activity.tvDevicename.setText("用户名:" + username);
                    activity.llConnectdevice.setVisibility(View.VISIBLE);
                }
                break;

                case BluetoothContral.MSG_CONNECTCENTRAL: {
                    LogUtil.e("收到消息了 MSG_CONNECTCENTRAL");
                    activity.CurrentDevice = (BluetoothDevice) msg.obj;

                }
                break;

                case BluetoothContral.MSG_DISCONNECT: {
                    LogUtil.e("收到消息了 MSG_DISCONNECT");
                    activity.llConnectdevice.setVisibility(View.GONE);
                    activity.tvConnectstat.setText("状态：未连接");
                    activity.tvConnectstat.setTextColor(Color.parseColor(activity.color_red));
                    activity.btOpenlock.setText("开锁");
                    activity.ivLock.setImageResource(R.drawable.close_lock_img);
                    activity.ivLocklight.setImageResource(R.drawable.light_red);
                    activity.tvLockstat.setText("锁头状态：关闭");
                    activity.lockStat = "f";
                    activity.mBluetoothContral.SetLockstat(activity.lockStat);
                    CacheUtils.putString(activity, BluetoothContral.KEY_LOCK_STAT, activity.lockStat);
                    activity.Vibrate(1000);
                }
                break;

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(stateReceiver);
        mBluetoothContral.StopAdvertise();
        mBluetoothContral.CloseBlue();
        mBluetoothContral = null;
        CurrentDevice = null;
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

    //Toast
    private void ShowText(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    //震动
    private void Vibrate(long time) {
        vibrator.vibrate(time);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && flag) {
            Toast.makeText(PeripheryActivity.this, "如果再点击一次，退出当前应用", Toast.LENGTH_SHORT).show();
            flag = false;
            //发送延迟消息
            handler.sendEmptyMessageDelayed(WHAT_RESET_BACK, 2000);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
