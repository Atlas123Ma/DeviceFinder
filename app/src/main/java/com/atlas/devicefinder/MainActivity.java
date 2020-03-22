package com.atlas.devicefinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

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

    }

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

    private static ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult, result = " + result);

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
        ScanFilter filter = new ScanFilter.Builder().setDeviceAddress("08:7C:BE:48:65:AD").setServiceUuid(ParcelUuid.fromString("0000fee7-0000-1000-8000-00805f9b34fb")).build();
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

