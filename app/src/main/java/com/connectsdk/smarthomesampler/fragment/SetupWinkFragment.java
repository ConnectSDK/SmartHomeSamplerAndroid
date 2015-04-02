/*
 * SetupWinkFragment
 * SmartHomeSamplerAndroid
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 30 Mar 2015
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

import com.connectsdk.core.Util;
import com.connectsdk.smarthomesampler.adapter.WinkAdapter;
import com.connectsdk.smarthomesampler.scene.WinkCredentials;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SetupWinkFragment extends SetupMultyChoiceFragment<JSONObject> {

    private WinkAdapter winkAdapter;

    public interface ISaveWinkDevice {
        public void saveWinkDevice(List<String> bulbs);
    }

    public static Fragment newInstance(ArrayList<String> bulbs) {
        SetupWinkFragment fragment = new SetupWinkFragment();
        return SetupMultyChoiceFragment.setArguments(fragment, bulbs);
    }

    @Override
    void startDeviceDiscovery() {
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    WinkCredentials credentials = new WinkCredentials();
                    winkAdapter.connect(credentials.getCredentials());
                    final List<JSONObject> bulbs = winkAdapter.getGeBulbIds();
                    updateData(bulbs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    void stopDeviceDiscovery() {

    }

    @Override
    void save() {
        ((ISaveWinkDevice)getActivity()).saveWinkDevice(ids);
    }

    @Override
    String getDeviceId(JSONObject device) {
        try {
            return device.getString("light_bulb_id");
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    String getDeviceName(JSONObject device) {
        try {
            return device.getString("name");
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    String getTitleMessage() {
        return "GE bulbs";
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        winkAdapter = new WinkAdapter();
    }
}
