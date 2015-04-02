/*
 * BluetoothService
 * SmartHomeSamplerAndroid
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 27 Mar 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.connectsdk.smarthomesampler;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BluetoothService extends Service implements BluetoothAdapter.LeScanCallback {
    public static final String ACTION_NEW_BEACON = "com.connectsdk.com.connectsdk.smarthomesampler.BluetoothService.action_new_beacon";
    public static final String ACTION_PAUSE_SCANNING = "com.connectsdk.com.connectsdk.smarthomesampler.BluetoothService.action_pause_scanning";
    
    private BluetoothAdapter btAdapter;
    
    private final Map<String, ScannedBleDevice> devices = new HashMap<String, ScannedBleDevice>();

    private Handler handler = new Handler();

    private final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            stopScan();
        }
    };
    
    private final Runnable startRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };
    private double prevDistance = 100;

    public BluetoothService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        BluetoothManager bt = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bt.getAdapter();

        startScan();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_PAUSE_SCANNING.equals(intent.getAction())) {
            stop();
            handler.postDelayed(startRunnable, 1000);
            btAdapter.disable();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }
    
    void stop() {
        handler.removeCallbacks(startRunnable);
        handler.removeCallbacks(stopRunnable);
        try {
            btAdapter.stopLeScan(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        btAdapter.disable();
    }


    void startScan() {
        if (btAdapter.isEnabled()) {
            try {
                btAdapter.startLeScan(new UUID[]{
                        UUID.fromString("00001804-0000-1000-8000-00805f9b34fb")}, this);
            } catch (Exception e) {
                e.printStackTrace();
            }

            handler.postDelayed(stopRunnable, 200);
        } else {
            btAdapter.enable();
            handler.postDelayed(startRunnable, 200);
        }
    }

    void stopScan() {
        btAdapter.stopLeScan(this);
        handler.postDelayed(startRunnable, 300);
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.d("", "BTSERV " + device.getAddress() + " " + rssi + " " + device.getName());
        ScannedBleDevice info = parseRawScanRecord(device, rssi, scanRecord);
        if (info != null) {
            Log.d("", "BTSERV " + info.Tx + " " + info.Distance);
            devices.put(device.getAddress(), info);
        }
        
        ScannedBleDevice closest = null;
        double min = Double.MAX_VALUE;
        for (ScannedBleDevice d : devices.values()) {
            if (min > d.Distance) {
                min = d.Distance;
                closest = d;
            }
        }

        if (closest != null) {
            Log.d("", "scene beacon closests yes ");
            if (Math.abs(prevDistance - closest.Distance) < 1.2 && closest.Distance < 1.8) {
                Intent intent = new Intent();
                intent.setAction(ACTION_NEW_BEACON);
                intent.putExtra("beacon", closest.MacAddress);
                intent.putExtra("distance", closest.Distance);
                Log.d("", "scene beacon broadcast ");
                sendBroadcast(intent);
            }

            prevDistance = closest.Distance;
        }
    }


    // an object with all information embedded from LeScanCallback data
    class ScannedBleDevice implements Serializable {
        public String MacAddress;

        public String DeviceName;
        public double RSSI;
        public double Distance;

        public byte[] CompanyId;
        public byte[] IbeaconProximityUUID;
        public byte[] Major;
        public byte[] Minor;
        public byte Tx;

        public long ScannedTime;
    }

    // use this method to parse those bytes and turn to an object which defined proceeding.
    // the uuidMatcher works as a UUID filter, put null if you want parse any BLE advertising data around.
    private ScannedBleDevice parseRawScanRecord(BluetoothDevice device, int rssi, byte[] advertisedData) {
        try {
            ScannedBleDevice parsedObj = new ScannedBleDevice();
            // parsedObj.BLEDevice = device;
            parsedObj.DeviceName = device.getName();
            parsedObj.MacAddress = device.getAddress();
            parsedObj.RSSI = rssi;

            int skippedByteCount = advertisedData[0];
            int magicStartIndex = skippedByteCount + 1;
            int magicEndIndex = magicStartIndex
                    + advertisedData[magicStartIndex] + 1;
            ArrayList<Byte> magic = new ArrayList<Byte>();
            for (int i = magicStartIndex; i < magicEndIndex; i++) {
                magic.add(advertisedData[i]);
            }

            byte[] companyId = new byte[2];
            companyId[0] = magic.get(2);
            companyId[1] = magic.get(3);
            parsedObj.CompanyId = companyId;

            byte[] ibeaconProximityUUID = new byte[16];
            for (int i = 0; i < 16; i++) {
                ibeaconProximityUUID[i] = magic.get(i + 6);
            }

            parsedObj.IbeaconProximityUUID = ibeaconProximityUUID;

            byte[] major = new byte[2];
            major[0] = magic.get(22);
            major[1] = magic.get(23);
            parsedObj.Major = major;

            byte[] minor = new byte[2];
            minor[0] = magic.get(24);
            minor[1] = magic.get(25);
            parsedObj.Minor = minor;

            byte tx;
            tx = magic.get(26);
            parsedObj.Tx = tx;
            parsedObj.Distance = getDistance(rssi, tx);
            parsedObj.ScannedTime = new Date().getTime();
            return parsedObj;
        } catch (Exception ex) {
            return null;
        }
    }

    private double getDistance(int rssi, int txPower) {
        /*
         * RSSI = TxPower - 10 * n * lg(d)
         * n = 2 (in free space)
         *
         * d = 10 ^ ((TxPower - RSSI) / (10 * n))
         */

        return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }
}
