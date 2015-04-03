/*
 * BeaconAdapter
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
package com.connectsdk.smarthomesampler.adapter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BeaconAdapter implements BluetoothAdapter.LeScanCallback {

    private final BluetoothAdapter btAdapter;

    private final Map<String, ScannedBleDevice> devices = new HashMap<String, ScannedBleDevice>();

    private double prevDistance = 100;

    private final Handler handler = new Handler();

    private final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            stop();
        }
    };

    private final Runnable startRunnable = new Runnable() {
        @Override
        public void run() {
            startScan();
        }
    };

    private BeaconUpdate listener;

    public interface BeaconUpdate {

        public void onClosestBeacon(ScannedBleDevice ble);

        public void onDetectBeacon(ScannedBleDevice ble);
    }

    public BeaconAdapter(Context context) {
        BluetoothManager bt = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bt.getAdapter();
    }

    public void setListener(BeaconUpdate listener) {
        this.listener = listener;
    }

    @SuppressWarnings("deprecation")
    public void startScan() {
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

    @SuppressWarnings("deprecation")
    private void stop() {
        btAdapter.stopLeScan(this);
        handler.postDelayed(startRunnable, 300);
    }

    @SuppressWarnings("deprecation")
    public void stopScan() {
        handler.removeCallbacks(startRunnable);
        handler.removeCallbacks(stopRunnable);
        try {
            btAdapter.stopLeScan(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        btAdapter.disable();
    }

    public void stopScanForPeriod(long time) {
        stopScan();
        handler.postDelayed(startRunnable, time);
        btAdapter.disable();
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        ScannedBleDevice info = parseRawScanRecord(device, rssi, scanRecord);
        if (info != null) {
            devices.put(device.getAddress(), info);

            if (listener != null) {
                listener.onDetectBeacon(info);
            }
        }

        ScannedBleDevice closest = null;
        double min = Double.MAX_VALUE;
        for (ScannedBleDevice d : devices.values()) {
            if (min > d.distance) {
                min = d.distance;
                closest = d;
            }
        }

        if (closest != null) {
            if (listener != null
                    && Math.abs(prevDistance - closest.distance) < 1.2 && closest.distance < 1.8) {
                listener.onClosestBeacon(closest);
            }

            prevDistance = closest.distance;
        }
    }

    public static class ScannedBleDevice implements Serializable {
        public String macAddress;

        public String deviceName;
        public double RSSI;
        public double distance;

        public String uuid;
        public byte[] companyId;
        public byte[] ibeaconProximityUUID;
        public byte[] major;
        public byte[] minor;
        public byte tx;

        public long scannedTime;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ScannedBleDevice that = (ScannedBleDevice) o;
            return macAddress.equals(that.macAddress);
        }

        @Override
        public int hashCode() {
            return macAddress.hashCode();
        }
    }

    private ScannedBleDevice parseRawScanRecord(BluetoothDevice device, int rssi, byte[] advertisedData) {
        try {
            ScannedBleDevice parsedObj = new ScannedBleDevice();
            parsedObj.deviceName = device.getName();
            parsedObj.macAddress = device.getAddress();
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
            parsedObj.companyId = companyId;

            byte[] ibeaconProximityUUID = new byte[16];
            for (int i = 0; i < 16; i++) {
                ibeaconProximityUUID[i] = magic.get(i + 6);
            }

            parsedObj.ibeaconProximityUUID = ibeaconProximityUUID;
            parsedObj.uuid = byteArrayToHex(ibeaconProximityUUID);

            byte[] major = new byte[2];
            major[0] = magic.get(22);
            major[1] = magic.get(23);
            parsedObj.major = major;

            byte[] minor = new byte[2];
            minor[0] = magic.get(24);
            minor[1] = magic.get(25);
            parsedObj.minor = minor;

            byte tx;
            tx = magic.get(26);
            parsedObj.tx = tx;
            parsedObj.distance = getDistance(rssi, tx);
            parsedObj.scannedTime = new Date().getTime();
            return parsedObj;
        } catch (Exception ex) {
            return null;
        }
    }

    private String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
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
