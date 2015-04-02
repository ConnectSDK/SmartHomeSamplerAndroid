/*
 * SetupBeaconFragment
 * SmartHomeSamplerAndroid
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 31 Mar 2015
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

package com.connectsdk.smarthomesampler.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.connectsdk.smarthomesampler.adapter.BeaconAdapter;

public class SetupBeaconFragment extends SetupSingleChoiceFragment<BeaconAdapter.ScannedBleDevice> implements BeaconAdapter.BeaconUpdate {

    private BeaconAdapter beaconAdapter;
    private String beaconMAC;

    public interface ISaveBeaconDevice {
        void saveBeaconDevice(BeaconAdapter.ScannedBleDevice device);
    }

    public static Fragment newInstance(String beaconMAC) {
        SetupBeaconFragment fragment = new SetupBeaconFragment();
        Bundle args = new Bundle();
        args.putString("beaconMAC", beaconMAC);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            beaconMAC = args.getString("beaconMAC");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        beaconAdapter = new BeaconAdapter(activity);
    }

    @Override
    public void startDeviceDiscovery() {
        beaconAdapter.setListener(this);
        beaconAdapter.startScan();
    }

    @Override
    public void stopDeviceDiscovery() {
        beaconAdapter.stopScan();
    }

    @Override
    void save(BeaconAdapter.ScannedBleDevice device) {
        ((ISaveBeaconDevice)getActivity()).saveBeaconDevice(device);
    }

    @Override
    String getDeviceName(BeaconAdapter.ScannedBleDevice device) {
        return (device.deviceName == null ? "" : device.deviceName) + " [" + device.macAddress + "]";
    }

    @Override
    String getTitleMessage() {
        return "iBeacon devices";
    }

    @Override
    int getPosition() {
        for (int i = 0; i < adapter.getCount(); i++) {
            BeaconAdapter.ScannedBleDevice device = adapter.getItem(i);
            if (device.macAddress.equals(beaconMAC)) {
                return i;
            }
        }
        return -1;
    }


    @Override
    public void onClosestBeacon(BeaconAdapter.ScannedBleDevice ble) {
    }

    @Override
    public void onDetectBeacon(BeaconAdapter.ScannedBleDevice ble) {
        updateData(ble);
    }

}
