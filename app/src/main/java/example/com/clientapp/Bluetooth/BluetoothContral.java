package example.com.clientapp.Bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Vibrator;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import example.com.clientapp.Utils.AESbyte;
import example.com.clientapp.Utils.CacheUtils;
import example.com.clientapp.Utils.LogUtil;
import example.com.clientapp.Utils.MD5Encoder;

import static android.content.Context.BLUETOOTH_SERVICE;
import static java.lang.Math.abs;
import static java.lang.Math.decrementExact;
import static java.lang.Math.pow;

/**
 * Created by Administrator on 2018/3/5.
 */

public class BluetoothContral {

    private Activity mContext;
    private  BluetoothManager mBluetoothManager;
    private  BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAvertiser;
    private BluetoothLeScanner mBluetoothScanner;
    private BluetoothDevice currentDevice;
    //中央设备核心操作
    private BluetoothGatt mBluetoothGatt;
    //外围设备核心操作
    private BluetoothGattServer mbBuetoothGattServer;

    //锁头和蓝牙锁状态
    private String lockStat = "";

    //异常中断标志
    private boolean atormatic = true;

    //连接标志
    private boolean IsConnect = false;

    //中心设备连接标志
    public boolean CIsConnect = true;

    //识别标志
    public String recognition = "f";

    private Handler mHandler;

    private String username = null;

    //加密Key随机生成
    private String Key = "";

    private Vibrator vibrator;

    private byte[] Scert;

    //UUID
    public static final String UUID_SERVER = "58015024-d8fa-453a-9302-50517c7ed4aa";//服务UUID
    public static final String UUID_C_LOCK_STAT = "0ebc1b9e-ae38-4c32-8681-af0bfbf4feb7";//锁头状态特征
    public static final String UUID_D_LOCK_D1 = "fd9c7bf7-62f1-4134-a709-fcd36f21da38";//锁头通知描述
    public static final String UUID_C_TKEY = "f315282c-8f04-499e-9873-f7a6f37ab731";//测试特征
    public static final String UUID_C_RECOGNITION = "245495f4-4bd0-42ec-958f-b95b78558986";//识别主人特征
    public static final String UUID_C_KEY = "b12031b9-3524-46cf-98df-5c35f4c1ae52";//密匙特征
    public static final String UUID_C_NAME="39fd18a1-340a-4e88-a591-f277227d3e7b";//传递名字

    //消息常量
    public static final int MSG_SCAN_DEVICE = 1;
    public static final int MSG_CONNECTCENTRAL = 2;
    public static final int MSG_DISCONNECT = 3;
    public static final int MSG_LOCK_STAT = 4;
    public static final int MSG_DISTANT = 6;
    public static final int MSG_RECOGNITION = 7;
    public static final int MSG_THREAD = 8;
    public static final int MSG_USERNAME = 9;
    public static final int MSG_TKEY = 10;

    //键值对
    public static final String KEY_DEVICE_NAME = "devicename";
    public static final String KEY_RSSI = "rssi";
    public static  final String KEY_LOCK_STAT = "lockstat";


    public BluetoothContral(Activity Context) {

        mContext = Context;
        vibrator = (Vibrator) mContext.getSystemService(mContext.VIBRATOR_SERVICE);
        String s = CacheUtils.getString(Context,"password");
        if ("".equals(s) || s == null){
            LogUtil.e("无存储密码");
            byte[] bb = {49,49,49};
            Scert = bb;
        }else{
            LogUtil.e("已经存过密码");
            Scert = s.getBytes();
            for (int i=0 ; i<Scert.length ; i++){
                LogUtil.e("获取的密码："+Scert[i]);
            }
        }



        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(BLUETOOTH_SERVICE);
        }

        if (mBluetoothManager != null && mBluetoothAdapter == null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        LogUtil.e("蓝牙管理器初始化");
    }

    //检查蓝牙
    public boolean CheckSupportBLE(){
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(mContext, "不支持ble", Toast.LENGTH_LONG).show();
            LogUtil.e("系统不支持");
            return false;
        }
        if (mBluetoothAdapter == null){
            Toast.makeText(mContext, "不支持ble", Toast.LENGTH_LONG).show();
            LogUtil.e("蓝牙不支持");
            return false;
        }

        return  true;
    }

    //打开蓝牙
    public void OpenBlue(){
        if (mBluetoothAdapter != null){
            mBluetoothAdapter.enable();
        }
    }

    //关闭蓝牙
    public void CloseBlue(){
        if (mBluetoothAdapter != null){
            mBluetoothAdapter.disable();
        }
    }

    //蓝牙是否打开
    public boolean IsOpen(){
        if (mBluetoothAdapter != null){
            return mBluetoothAdapter.isEnabled();
        }
        return false;
    }

    //初始化广播及开始广播
    public void InitGATTServer(){

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setConnectable(true)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH )
                .build();

        //广播数据
        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        //响应数据
        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid.fromString(UUID_SERVER))
                .build();

        if (mBluetoothLeAvertiser == null && mBluetoothAdapter != null){
            mBluetoothLeAvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mBluetoothLeAvertiser.startAdvertising(settings,advertiseData,scanResponseData,advertiseCallback);
        }else{
            mBluetoothLeAvertiser.startAdvertising(settings,advertiseData,scanResponseData,advertiseCallback);
        }
    }

    //停止广播
    public void StopAdvertise(){
        if (mBluetoothLeAvertiser != null ){
            ShowText("停止广播");
            LogUtil.e("停止广播");
            mBluetoothLeAvertiser.stopAdvertising(advertiseCallback);
            mbBuetoothGattServer.cancelConnection(currentDevice);
            mbBuetoothGattServer.close();
            mbBuetoothGattServer = null;
        }
    }

    //注册Handler
    public void RegesteHandler(Handler Handler){
        mHandler = Handler;
    }

    //初始化服务
    private void InitServices(Activity mContext) {
        mbBuetoothGattServer = mBluetoothManager.openGattServer(mContext,bluetoothGattServerCallback);
        BluetoothGattService  serviceMain = new BluetoothGattService(UUID.fromString(UUID_SERVER)
                ,BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //锁头状态特征 读
         BluetoothGattCharacteristic characteristicL   = new BluetoothGattCharacteristic(UUID.fromString(UUID_C_LOCK_STAT)
        ,BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ );

         //锁头状态通知描述 写
         BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID.fromString(UUID_D_LOCK_D1),BluetoothGattDescriptor.PERMISSION_WRITE);
         characteristicL.addDescriptor(descriptor);

        //传输密码特征 写
        BluetoothGattCharacteristic characteristicD = new BluetoothGattCharacteristic(UUID.fromString(UUID_C_TKEY),
                BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE);

        //识别主人特征 写 读
        BluetoothGattCharacteristic characteristicR = new BluetoothGattCharacteristic(UUID.fromString(UUID_C_RECOGNITION),
                BluetoothGattCharacteristic.PROPERTY_WRITE|BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_WRITE|BluetoothGattCharacteristic.PERMISSION_READ);

        //密匙特征 写
        BluetoothGattCharacteristic characteristicK = new BluetoothGattCharacteristic(UUID.fromString(UUID_C_KEY),
                BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE);

        //名字特征 写
        BluetoothGattCharacteristic characteristicN = new BluetoothGattCharacteristic(UUID.fromString(UUID_C_NAME),
                BluetoothGattCharacteristic.PROPERTY_WRITE,BluetoothGattCharacteristic.PERMISSION_WRITE);

        serviceMain.addCharacteristic(characteristicD);
        serviceMain.addCharacteristic(characteristicL);
        serviceMain.addCharacteristic(characteristicR);
        serviceMain.addCharacteristic(characteristicK);
        serviceMain.addCharacteristic(characteristicN);


        mbBuetoothGattServer.addService(serviceMain);
        LogUtil.e("服务初始化成功");
    }

    //开始扫描
    public void StartScan() {
        List<ScanFilter> bleScanFilters = new ArrayList<>();
        bleScanFilters.add(
                new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(UUID_SERVER)).build()
        );
        ScanSettings bleScanSetting = new ScanSettings.Builder().build();
        if (mBluetoothAdapter != null) {
            LogUtil.e("开始扫描");
            ShowText("开始扫描");
            if (mBluetoothScanner == null) {
                mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
                mBluetoothScanner.startScan(bleScanFilters,bleScanSetting,ScanCallback);
            } else {
                mBluetoothScanner.startScan(bleScanFilters,bleScanSetting,ScanCallback);
            }
        }
    }

    //停止扫描
    public void StopScan(){
        if (mBluetoothScanner != null){
            LogUtil.e("停止扫描");
            mBluetoothScanner.stopScan(ScanCallback);
        }
    }

    //广播回调
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            LogUtil.e("广播成功");
            ShowText("广播成功");
            InitServices(mContext);
        }

        @Override
        public void onStartFailure(int errorCode) {
            LogUtil.e("广播失败: " + errorCode);
            ShowText("广播失败");
        }
    };

    //回调服务响应事件
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange( BluetoothDevice device, int status, int newState) {
            LogUtil.e("onConnectionStateChange 连接状态改变");
            switch (newState){
                case BluetoothProfile.STATE_CONNECTED: {
                    LogUtil.e("连接成功");
                    currentDevice = device;
                    IsConnect = true;
                    Message msg = mHandler.obtainMessage(MSG_CONNECTCENTRAL);
                    LogUtil.e("" + device.getName());
                    msg.obj = device;
                    mHandler.sendMessage(msg);
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ShowText("连接成功");
                        }
                    });
                    LogUtil.e("发消息了 MSG_CONNECTCENTRAL");
                }
                    break;

                case BluetoothProfile.STATE_DISCONNECTED: {
                    LogUtil.e("关闭连接");
                    IsConnect = false;
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ShowText("关闭连接");
                        }
                    });
                    Message msg = mHandler.obtainMessage(MSG_DISCONNECT);
                    mHandler.sendMessage(msg);
                    LogUtil.e("发消息了 MSG_DISCONNECT");

                }
                    break;
            }

        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            LogUtil.e("onServiceAdded 成功添加服务");

        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            switch (characteristic.getUuid().toString()){

                case UUID_C_RECOGNITION:{
                    LogUtil.e("CharacteristicReadReq 远程设备请求读取数据 UUID_C_RECOGNITION");
                    LogUtil.e(""+recognition);
                    mbBuetoothGattServer.sendResponse(device,requestId,offset,BluetoothGatt.GATT_SUCCESS,recognition.getBytes());
                }
                    break;

                case UUID_C_LOCK_STAT: {

                    if (recognition.equals("t")) {
                        LogUtil.e("CharacteristicReadReq 远程设备请求读取数据 UUID_LOCK_STAT");
                        LogUtil.e("" + lockStat);
                        mbBuetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, lockStat.getBytes());
                    }
                }
                    break;

            }
            super.onCharacteristicReadRequest(device,requestId,offset,characteristic);
        }


        @Override
        public void onCharacteristicWriteRequest  (BluetoothDevice device, final int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, final byte[] requestBytes) {
                    switch (characteristic.getUuid().toString()) {
                        case UUID_C_NAME:{
                            LogUtil.e("onCharacteristicWriteRequest 远程设备请求写入特征 UUID_C_NAME");
                            String s = new String(requestBytes);
                            LogUtil.e(s);
                            Message message = mHandler.obtainMessage(MSG_USERNAME);
                            message.obj = s;
                            mHandler.sendMessage(message);
                            LogUtil.e("发消息了 MSG_USERNAME");
                            mbBuetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{1, 2, 3});
                        }
                        break;

                        case UUID_C_TKEY: {
                            LogUtil.e("onCharacteristicWriteRequest 远程设备请求写入特征 UUID_C_TKEY");
                            Scert = requestBytes;
                            for (int i=0 ; i<Scert.length ; i++){
                                LogUtil.e("得到的密码："+Scert[i]);
                            }
                            String s = new String(requestBytes);
                            LogUtil.e(s);
                            CacheUtils.putString(mContext,"password",s);
                            mContext.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ShowText("密码被修改");
                                }
                            });
                            Message message = mHandler.obtainMessage(MSG_TKEY);
                            message.obj = s;
                            mHandler.sendMessage(message);
                            LogUtil.e("发消息了 UUID_C_TKEY");
                            mbBuetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{1, 2, 3});
                        }
                        break;

                        case UUID_C_RECOGNITION: {
                            LogUtil.e("onCharacteristicWriteRequest 远程设备请求写入特征 UUID_C_RECOGNITION");

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        byte[] bb = MD5Encoder.decrypt(requestBytes, Key.getBytes());
                                        for (int i=0 ; i < bb.length ; i++){
                                            LogUtil.e("解析传输:"+bb[i]);
                                        }
                                        if(Arrays.equals(Scert,bb)){
                                            LogUtil.e("验证正确");
                                            recognition = "t";
                                        }else{
                                            LogUtil.e("验证错误");
                                            recognition = "f";
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    Message message = mHandler.obtainMessage(MSG_RECOGNITION);
                                    message.obj = recognition;
                                    mHandler.sendMessage(message);
                                    LogUtil.e("发消息了 MSG_RECOGNITION");
                                }
                            });


                            mbBuetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,new byte[]{1,2,3});
                        }
                            break;

                        case UUID_C_KEY:{
                            LogUtil.e("onCharacteristicWriteRequest 远程设备请求写入特征 UUID_C_KEY");
                            final String s = new String(requestBytes);
                            Key = s;
                            LogUtil.e(Key);
                            mbBuetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,new byte[]{1,2,3,4,5,6,7,8,9,10});
                        }
                            break;
                    }


        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            LogUtil.e("DescriptorReadReq 远程设备请求读取描述器");


        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            switch (descriptor.getUuid().toString()){
                case UUID_D_LOCK_D1:{
                    LogUtil.e("DescriptorReadReq 远程设备请求写入描述器 UUID_D_LOCK_D1");
                    mbBuetoothGattServer.sendResponse(device,requestId,BluetoothGatt.GATT_SUCCESS,offset,descriptor.getValue());
                }
                    break;
            }
            super.onDescriptorWriteRequest(device,requestId,descriptor,preparedWrite,responseNeeded,offset,value);

        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            LogUtil.e("onExecuteWrite 执行挂起写入操作");
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            LogUtil.e("onNotificationSent 通知发送");
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            LogUtil.e("onMtuChanged mtu改变");
        }
    };


    //扫描回调
    private ScanCallback ScanCallback = new ScanCallback(){
        @Override
        public void onScanResult(final int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            int rssi = result.getRssi();
            double dis = CalcDistByRSSI(rssi);
            DecimalFormat decimalFormat = new DecimalFormat("#.00");
            //实际距离保留小数点后两位
            String disitance = decimalFormat.format(dis);

            if (Float.parseFloat(disitance) > 1.0 && CIsConnect){

                ScanRecord scanRecord = result.getScanRecord();

                final Message msg = mHandler.obtainMessage(MSG_DISTANT);
                Bundle bundle = new Bundle();

                //设备名  信号强度 Txpower
                bundle.putString(KEY_DEVICE_NAME, scanRecord.getDeviceName());
                bundle.putInt(KEY_RSSI, result.getRssi());

                msg.setData(bundle);
                mHandler.sendMessage(msg);
                LogUtil.e("发消息了 MSG_DISTANT");

            }else {

                BluetoothDevice device = result.getDevice();
                ScanRecord scanRecord = result.getScanRecord();

                final Message msg = mHandler.obtainMessage(MSG_SCAN_DEVICE);
                Bundle bundle = new Bundle();

                //设备名  信号强度 Txpower
                bundle.putString(KEY_DEVICE_NAME, scanRecord.getDeviceName());
                bundle.putInt(KEY_RSSI, result.getRssi());

                msg.setData(bundle);
                mHandler.sendMessage(msg);
                LogUtil.e("发消息了 MSG_SCAN_DEVICE");

                if (CIsConnect) {
                    CIsConnect = false;
                    //连接设备回调
                    mBluetoothGatt = device.connectGatt(mContext, false, new BluetoothGattCallback() {
                        @Override
                        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                            LogUtil.e("onConnectionStateChange 连接状态改变");

                            switch (newState) {
                                case BluetoothProfile.STATE_CONNECTED: {
                                    LogUtil.e("连接成功");
                                    atormatic = true;
                                    LogUtil.e("" + atormatic);
                                    mContext.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ShowText("连接成功");
                                        }
                                    });
                                    gatt.discoverServices();
                                }
                                break;

                                case BluetoothProfile.STATE_DISCONNECTED: {
                                    LogUtil.e("" + atormatic);
                                    CIsConnect = true;
                                    if (atormatic) {
                                        LogUtil.e("异常中断");
                                        mContext.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ShowText("异常中断，正在自动重连");
                                            }
                                        });
                                        Message msg = mHandler.obtainMessage(MSG_THREAD);
                                        LogUtil.e("发消息了 MSG_THREAD");
                                        mHandler.sendMessage(msg);
                                    } else {
                                        LogUtil.e("关闭连接");
                                        mContext.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ShowText("关闭连接");
                                            }
                                        });

                                        Message msg = mHandler.obtainMessage(MSG_DISCONNECT);
                                        mHandler.sendMessage(msg);
                                        LogUtil.e("发消息了 MSG_DISCONNECT");
                                        mBluetoothGatt.disconnect();
                                        mBluetoothGatt.close();
                                    }
                                }
                                break;
                            }
                        }

                        @Override
                        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {

                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                //先把密匙传过去
                                BluetoothGattService serviceMain = mBluetoothGatt.getService(UUID.fromString(UUID_SERVER));
                                BluetoothGattCharacteristic characteristic = serviceMain.getCharacteristic(UUID.fromString(UUID_C_KEY));
                                String key1 = AESbyte.generateKey();
                                Key = key1.substring(0, 18);
                                LogUtil.e(Key);
                                characteristic.setValue(Key.getBytes());
                                writeCharacteristic(characteristic);
                            }
                        }

                        @Override
                        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            switch (characteristic.getUuid().toString()) {

                                case UUID_C_RECOGNITION: {
                                    LogUtil.e(" onCharacteristicRead UUID_C_RECOGNITION 回调了");
                                    String s = new String(characteristic.getValue());
                                    recognition = s;
                                    LogUtil.e(s);
                                    if (s.equals("f")) {
                                        mContext.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ShowText("识别错误 无法继续操作");
                                            }
                                        });
                                        Message msg = mHandler.obtainMessage(MSG_RECOGNITION);
                                        msg.obj = s;
                                        mHandler.sendMessage(msg);
                                        LogUtil.e("发消息了 MSG_RECOGNITION");

                                    } else if (s.equals("t")) {
                                        mContext.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                ShowText("识别成功");
                                            }
                                        });

                                        Message msg = mHandler.obtainMessage(MSG_RECOGNITION);
                                        msg.obj = s;
                                        mHandler.sendMessage(msg);
                                        LogUtil.e("发消息了 MSG_RECOGNITION");

                                        BluetoothGattService serviceMain = mBluetoothGatt.getService(UUID.fromString(UUID_SERVER));
                                        final BluetoothGattCharacteristic characteristicL = serviceMain.getCharacteristic(UUID.fromString(UUID_C_LOCK_STAT));

                                        BluetoothGattCharacteristic characteristicN = serviceMain.getCharacteristic(UUID.fromString(UUID_C_NAME));


                                        byte[] bb = username.getBytes();
                                        characteristicN.setValue(bb);
                                        //传输用户名
                                        writeCharacteristic(characteristicN);

                                        mHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                readCharacteristic(characteristicL);
                                            }
                                        }, 300);

                                        mBluetoothGatt.setCharacteristicNotification(characteristicL, true);
                                        final BluetoothGattDescriptor descriptor = characteristicL.getDescriptor(UUID.fromString(UUID_D_LOCK_D1));
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                        mHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (writeDescriptor(descriptor)) {
                                                    LogUtil.e("写了");

                                                } else {
                                                    LogUtil.e("没写");
                                                }
                                            }
                                        }, 3000);
                                    }
                                }
                                break;

                                case UUID_C_LOCK_STAT: {
                                    String s = new String(characteristic.getValue());
                                    Message msg = mHandler.obtainMessage(MSG_LOCK_STAT);
                                    msg.obj = s;
                                    LogUtil.e("lock_sta:" + s);
                                    mHandler.sendMessage(msg);
                                    LogUtil.e("发消息了 MSG_LOCK_STAT");
                                }
                                break;



                            }
                            super.onCharacteristicRead(gatt, characteristic, status);
                        }

                        @Override
                        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                            switch (characteristic.getUuid().toString()) {
                                case UUID_C_NAME: {
                                    LogUtil.e("onCharacteristicWrite UUID_C_NAME 回调了");
                                }
                                break;

                                case UUID_C_TKEY: {
                                    LogUtil.e("onCharacteristicWrite UUID_C_TKEY 回调了");
                                    mContext.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ShowText("可以继续操作");
                                        }
                                    });
                                }
                                break;

                                case UUID_C_RECOGNITION: {
                                    LogUtil.e("onCharacteristicWrite UUID_C_RECOGNITION 回调了");
                                    readCharacteristic(characteristic);
                                }
                                break;

                                case UUID_C_KEY: {
                                    LogUtil.e("onCharacteristicWrite UUID_C_KEY 回调了");
                                    BluetoothGattService serviceMain = mBluetoothGatt.getService(UUID.fromString(UUID_SERVER));
                                    BluetoothGattCharacteristic characteristic1 = serviceMain.getCharacteristic(UUID.fromString(UUID_C_RECOGNITION));
                                    byte[] bb = Scert;
                                    try {
                                        byte[] value = MD5Encoder.encrypt(bb, Key.getBytes());
                                        characteristic1.setValue(value);
                                        writeCharacteristic(characteristic1);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            super.onCharacteristicWrite(gatt, characteristic, status);
                        }

                        @Override
                        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                            switch (characteristic.getUuid().toString()) {
                                case UUID_C_LOCK_STAT: {
                                    String s = new String(characteristic.getValue());
                                    Message msg = mHandler.obtainMessage(MSG_LOCK_STAT);
                                    msg.obj = s;
                                    LogUtil.e("lock_sta:" + s);
                                    mHandler.sendMessage(msg);
                                }
                                break;
                            }
                            super.onCharacteristicChanged(gatt, characteristic);
                        }

                        @Override
                        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                            super.onDescriptorRead(gatt, descriptor, status);
                        }

                        @Override
                        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                            switch (descriptor.getUuid().toString()) {
                                case UUID_D_LOCK_D1: {
                                    LogUtil.e("onDescriptorWrite UUID_D_LOCK_D1 回调了");
                                }
                                break;
                            }
                            super.onDescriptorWrite(gatt, descriptor, status);
                        }
                    });
                }
            }

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            LogUtil.e("搜索失败");
        }
    };

    //根据信号强度计算距离
    public double CalcDistByRSSI(int rssi){
        int iRssi = abs(rssi);
        double power = (iRssi - 60) / ( 10 * 1.9);
        return pow(10,power);


    }

    //中央设备关闭连接
    public void CDisConnect(){
        if (mBluetoothGatt != null){
            LogUtil.e("中央关闭");
            CIsConnect = true;
            Message msg = mHandler.obtainMessage(MSG_DISCONNECT);
            mHandler.sendMessage(msg);
            LogUtil.e("发消息了 MSG_DISCONNECT");
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    //写特性
    private synchronized   boolean writeCharacteristic(BluetoothGattCharacteristic characteristic){
        if (mBluetoothGatt!=null){
            return mBluetoothGatt.writeCharacteristic(characteristic);
        }
        return false;
    }

    //读特性
    private synchronized boolean readCharacteristic(BluetoothGattCharacteristic characteristic){
        if (mBluetoothGatt!=null){
            return mBluetoothGatt.readCharacteristic(characteristic);
        }
        return false;
    }

    //写描述
    private synchronized boolean writeDescriptor(BluetoothGattDescriptor descriptor){
        if (mBluetoothGatt!=null){
            return mBluetoothGatt.writeDescriptor(descriptor);
        }
        return false;
    }

    //读特性
    private synchronized boolean readDescriptor(BluetoothGattDescriptor descriptor){
        if (mBluetoothGatt!=null){
            return mBluetoothGatt.readDescriptor(descriptor);
        }
        return false;
    }

    //设置用户名
    public void SetUsername(String s){
        username = s;
    }

    //设置锁头状态
    public void SetLockstat(String stat){
        lockStat = stat;
    }

    //客户端设置密码
    public void SetPassword(byte[] s){
        Scert = s;
    }

    //外围设备更新锁头状态
    public void UpLockstat(String value){
        if (mbBuetoothGattServer != null && IsConnect){

            BluetoothGattService service = mbBuetoothGattServer.getService(UUID.fromString(UUID_SERVER));
            BluetoothGattCharacteristic characteristicL = service.getCharacteristic(UUID.fromString(UUID_C_LOCK_STAT));
            characteristicL.setValue(value.getBytes());
            mbBuetoothGattServer.notifyCharacteristicChanged(currentDevice,characteristicL,false);
            LogUtil.e("更新锁头状态");
        }
    }

    //传输密码
    public void TKey(){
        BluetoothGattService severce = mBluetoothGatt.getService(UUID.fromString(UUID_SERVER));
        BluetoothGattCharacteristic c = severce.getCharacteristic(UUID.fromString(UUID_C_TKEY));

        c.setValue(Scert);
        writeCharacteristic(c);

    }

    public boolean getIsConnect(){
        return IsConnect;
    }

    //外围设备获得密码
    public String getScert(){
        String s = new String(Scert);

        return s;
    }

    //Toast
    private void ShowText(String s){
        Toast.makeText(mContext,s,Toast.LENGTH_SHORT).show();
    }
}
