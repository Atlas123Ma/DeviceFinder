package com.atlas.devicefinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter.*;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.atlas.devicefinder.LeXinUUID;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.*;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static  final String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeScanner mBluetoothLeScanner = null;
    private Button mBtStartBleScan1 = null;
    private Button mBtStopBleScan1 = null;
    private Button mBtStartBleScan2 = null;
    private Button mBtStopBleScan2 = null;
    private BluetoothDevice mTargetDevice = null;
    private BluetoothGatt mBluetoothGatt = null;
    private Handler mHandler = null;
    private static final int MSG_FIND_TARGET = 0;
    private static final int MSG_READ_CHARA = 1;
    private static final int MSG_WRITE_CHARA = 2;
    private static final int MSG_INDICATE = 3;
    private static final int DELAY_TIMES = 500;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        requestPermission();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "mBluetoothAdapter = " + mBluetoothAdapter);
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        Log.d(TAG, "mBluetoothLeScanner = " + mBluetoothLeScanner);

        mHandler = new Handler(){
            @NonNull
            @Override
            public void dispatchMessage(Message msg) {
                Log.d(TAG, "dispatchMessage, msg = " + msg.what);
                switch (msg.what) {
                    case MSG_FIND_TARGET:
                        mTargetDevice = (BluetoothDevice) msg.obj;
                        stopBleScan2();
                        mBluetoothGatt = mTargetDevice.connectGatt(getApplicationContext(), false, mBluetoothGattCallback);
                        break;
                    case MSG_READ_CHARA:
                        BluetoothGattCharacteristic gattCharacteristic = (BluetoothGattCharacteristic) msg.obj;
                        mBluetoothGatt.readCharacteristic(gattCharacteristic);
                        break;
                    case MSG_WRITE_CHARA:
                        BluetoothGattCharacteristic gattCharacteristic2 = (BluetoothGattCharacteristic) msg.obj;
                        gattCharacteristic2.setValue("0");
                        mBluetoothGatt.writeCharacteristic(gattCharacteristic2);
                        break;
                    case MSG_INDICATE:
                        BluetoothGattCharacteristic gattCharacteristic3 = (BluetoothGattCharacteristic) msg.obj;
                        boolean ret =mBluetoothGatt.setCharacteristicNotification(gattCharacteristic3, true);
                        BluetoothGattDescriptor descriptor = gattCharacteristic3.getDescriptor(UUID.fromString(LeXinUUID.INDICATE_DES));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(descriptor);
                        break;
                }
            }
        };
    }


    private void parseService(List<BluetoothGattService> gattServiceList) {
        Log.d(TAG, "----- start parseServie -----");
        for (int i = 0; i < gattServiceList.size(); i++) {
            BluetoothGattService gattService = (BluetoothGattService)gattServiceList.get(i);
            UUID uuid = gattService.getUuid();
            Log.d(TAG, "parseService, service uuid = " + uuid);

            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (int j = 0; j < gattCharacteristics.size(); j++) {
                BluetoothGattCharacteristic gattCharacteristic = gattCharacteristics.get(j);

                int properties = gattCharacteristic.getProperties();
                int permission = gattCharacteristic.getPermissions();
                Log.d(TAG, "gattCharacteristic, characteristic uuid = " + gattCharacteristic.getUuid() + " properties = " + properties);
                if (0 !=  (properties & BluetoothGattCharacteristic.PROPERTY_READ)) {
                    Log.d(TAG, "gattCharacteristic, characteristic uuid = " + gattCharacteristic.getUuid() + " support read");
                    if (gattCharacteristic.getUuid().toString().equals(LeXinUUID.READ_CHARA)) {
                        Message msg = new Message();
                        msg.what = MSG_READ_CHARA;
                        msg.obj = gattCharacteristic;
                        mHandler.sendMessageDelayed(msg, DELAY_TIMES);
                    }

//                        mBluetoothGatt.readCharacteristic(gattCharacteristic);
                }
                if ((0 !=  (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE))) {
                    Log.d(TAG, "gattCharacteristic, characteristic uuid = " + gattCharacteristic.getUuid() + " support indicate");
                    if (gattCharacteristic.getUuid().toString().equals(LeXinUUID.INDICATE_CHARA)) {
                        Message msg = new Message();
                        msg.what = MSG_INDICATE;
                        msg.obj = gattCharacteristic;
                        mHandler.sendMessageDelayed(msg, 3*DELAY_TIMES);
                    }
                }

                if (0 !=  (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                    Log.d(TAG, "gattCharacteristic, characteristic uuid = " + gattCharacteristic.getUuid() + " support notify");
                }

                if ((0 !=  (properties & BluetoothGattCharacteristic.PROPERTY_WRITE))) {
                    Log.d(TAG, "gattCharacteristic, characteristic uuid = " + gattCharacteristic.getUuid() + " support write");
                    if (gattCharacteristic.getUuid().toString().equals(LeXinUUID.WRITE_CHARA)) {
                        Message msg = new Message();
                        msg.what = MSG_WRITE_CHARA;
                        msg.obj = gattCharacteristic;
                        mHandler.sendMessageDelayed(msg, 2*DELAY_TIMES);
                    }
                }
                List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();
                for (int k = 0; k < gattDescriptors.size(); k++) {
                    BluetoothGattDescriptor gattDescriptor = (BluetoothGattDescriptor)gattDescriptors.get(k);
                    Log.d(TAG, "gattDescriptors, descriptor uuid = " + gattDescriptor.getUuid());
//                        gattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
//                        mBluetoothGatt.writeDescriptor(gattDescriptor);
                }
            }
        }
        Log.d(TAG, "----- parseServie end -----");
    }

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {

        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite, descriptor = " + descriptor + ", status = " + status);
        }
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange, status = " + status + ", newState = " + newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mBluetoothGatt.discoverServices();
                } else {
                    mBluetoothGatt.close();
                    mBluetoothGatt = mTargetDevice.connectGatt(getApplicationContext(), false, mBluetoothGattCallback);
                }
            }
        }


        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered, status = " + status);
            List<BluetoothGattService> gattServicesList = mBluetoothGatt.getServices();
            parseService(gattServicesList);

        }

        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged, characteristic = " + characteristic + ", value = " + characteristic.getValue());
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite, characteristic = " + characteristic + ", value = " + characteristic.getStringValue(0) + ", status = " + status);
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead, characteristic = " + characteristic + ", status = " + status + ", value = " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0));
        }
    };
    private void initView() {
        mBtStartBleScan1 = (Button)findViewById(R.id.bt_start_blescan1);
        mBtStartBleScan1.setOnClickListener(this);
        mBtStopBleScan1 = (Button)findViewById(R.id.bt_stop_blescan1);
        mBtStopBleScan1.setOnClickListener(this);
        mBtStartBleScan2 = (Button)findViewById(R.id.bt_start_blescan2);
        mBtStartBleScan2.setOnClickListener(this);
        mBtStopBleScan2 = (Button)findViewById(R.id.bt_stop_blescan2);
        mBtStopBleScan2.setOnClickListener(this);
    }
    private static LeScanCallback mLeScanCallback = new LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(device == null || device.getName() == null || device.getAddress() == null) {
                return;
            }
            Log.d(TAG, "onLeScan(), device = " + device.getName() +  "mac = " + device.getAddress() + "rssi = " + rssi);
            for (int i = 0; i < scanRecord.length; i++) {
                Log.d(TAG, "onLeScan(), device = " + device.getName() + "content = " + scanRecord[i]);
            }
        }
    };
    private void startBleScan1() {
        boolean ret = mBluetoothAdapter.startLeScan(mLeScanCallback);
        Log.d(TAG, "startBtScan(),ret = " + ret);
    }

    private void stopBleSscan1() {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }


    private  ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult, result = " + result);
            Message msg = new Message();
            msg.what = MSG_FIND_TARGET;
            msg.obj = result.getDevice();
            mHandler.sendMessage(msg);

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "onBatchScanResults");

        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "onScanFailed, errorCode = " + errorCode);
        }
    };
    private void startBleScan2() {
//        mBluetoothLeScanner.startScan(mScanCallback);

        //过滤条件
        List<ScanFilter> bleScanFilters = new ArrayList<>();
//        ScanFilter filter = new ScanFilter.Builder().setDeviceAddress("08:7C:BE:48:65:AD").setServiceUuid(ParcelUuid.fromString("0000fee7-0000-1000-8000-00805f9b34fb")).build();
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("0000FEE7-0000-1000-8000-00805F9B34FB")).build();
        bleScanFilters.add(filter);
        //扫描设置
        ScanSettings scanSetting = new ScanSettings.Builder().setScanMode(SCAN_MODE_LOW_LATENCY).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).setMatchMode(ScanSettings.MATCH_MODE_STICKY).build();
        mBluetoothLeScanner.startScan(bleScanFilters, scanSetting, mScanCallback);
    }

    private void stopBleScan2() {
        mBluetoothLeScanner.stopScan(mScanCallback);
    }
    private void requestPermission() {
        if (Build.VERSION.SDK_INT < 23){return;}
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public void onClick(View v) {
        Log.d(TAG, "onClick,View = " + v.toString());
        switch (v.getId()) {
            case R.id.bt_start_blescan1:
                startBleScan1();
                break;
            case R.id.bt_stop_blescan1:
                stopBleSscan1();
                break;
            case R.id.bt_start_blescan2:
                startBleScan2();
                break;
            case R.id.bt_stop_blescan2:
                stopBleScan2();
                break;
        }
    }
}

