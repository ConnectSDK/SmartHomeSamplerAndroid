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

import com.connectsdk.smarthomesampler.adapter.BeaconAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BluetoothService extends Service implements BeaconAdapter.BeaconUpdate {
    public static final String ACTION_NEW_BEACON = "com.connectsdk.com.connectsdk.smarthomesampler.BluetoothService.action_new_beacon";
    public static final String ACTION_PAUSE_SCANNING = "com.connectsdk.com.connectsdk.smarthomesampler.BluetoothService.action_pause_scanning";
    
    BeaconAdapter beaconAdapter;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        beaconAdapter = new BeaconAdapter(getApplicationContext());
        beaconAdapter.setListener(this);
        beaconAdapter.startScan();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_PAUSE_SCANNING.equals(intent.getAction())) {
            beaconAdapter.stopScanForPeriod(1000);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        beaconAdapter.stopScan();
    }

    @Override
    public void onClosestBeacon(BeaconAdapter.ScannedBleDevice ble) {
        if (ble != null) {
            Intent intent = new Intent();
            intent.setAction(ACTION_NEW_BEACON);
            intent.putExtra("beacon", ble.macAddress);
            intent.putExtra("distance", ble.distance);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onDetectBeacon(BeaconAdapter.ScannedBleDevice ble) {
    }
}
