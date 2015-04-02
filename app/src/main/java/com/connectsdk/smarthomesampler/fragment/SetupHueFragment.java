/*
 * SetupHueFragment
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
import android.support.v4.app.Fragment;

import com.connectsdk.smarthomesampler.adapter.HueAdapter;
import com.connectsdk.smarthomesampler.dialog.HueFragmentDialog;
import com.philips.lighting.model.PHLight;

import java.util.ArrayList;
import java.util.List;

public class SetupHueFragment extends SetupMultyChoiceFragment<PHLight> implements HueAdapter.HueListener {

    private HueAdapter hueAdapter;

    public interface ISaveHueDevice {
        public void saveHueDevice(List<String> bulbs);
    }

    public static Fragment newInstance(ArrayList<String> bulbs) {
        SetupHueFragment fragment = new SetupHueFragment();
        return SetupMultyChoiceFragment.setArguments(fragment, bulbs);
    }

    @Override
    void startDeviceDiscovery() {
        hueAdapter.addConnectListener(this);
        hueAdapter.startDiscovery();
    }

    @Override
    void stopDeviceDiscovery() {
        hueAdapter.stopDiscovery();
    }

    @Override
    void save() {
        ((ISaveHueDevice)getActivity()).saveHueDevice(ids);
    }

    @Override
    String getDeviceId(PHLight device) {
        return device.getIdentifier();
    }

    @Override
    String getDeviceName(PHLight device) {
        return device.getName();
    }

    @Override
    String getTitleMessage() {
        return "Philips Hue bulbs";
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        hueAdapter = new HueAdapter(getActivity());
    }

    @Override
    public void onHueConnected() {
        if (adapter != null && adapter.isEmpty()) {
            updateData(hueAdapter.getAllBulbs());
        }
    }

    @Override
    public void onHuePairingRequest(String name) {
        HueFragmentDialog.newInstance().show(getActivity().getSupportFragmentManager(), "hue_dialog");
    }

}
