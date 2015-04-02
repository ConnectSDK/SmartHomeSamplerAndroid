/*
 * SetupSceneActivity
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
package com.connectsdk.smarthomesampler;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.DLNAService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.smarthomesampler.adapter.BeaconAdapter;
import com.connectsdk.smarthomesampler.fragment.SetupBeaconFragment;
import com.connectsdk.smarthomesampler.fragment.SetupHueFragment;
import com.connectsdk.smarthomesampler.fragment.SetupMediaFragment;
import com.connectsdk.smarthomesampler.fragment.SetupWeMoFragment;
import com.connectsdk.smarthomesampler.fragment.SetupWinkFragment;
import com.connectsdk.smarthomesampler.scene.SceneConfig;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class SetupSceneActivity extends ActionBarActivity implements SetupMediaFragment.ISaveConnectableDevice,
        SetupHueFragment.ISaveHueDevice, SetupWeMoFragment.ISaveWemoDevice,
        SetupBeaconFragment.ISaveBeaconDevice, SetupWinkFragment.ISaveWinkDevice {

    public static final String EXTRA_SCENE_ID = "extra_scene_id";

    @InjectView(R.id.viewPager)
    ViewPager viewPager;

    private Fragment[] pages = new Fragment[5];

    private SceneConfig sceneConfig;

    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_setup);
        ButterKnife.inject(this);
        id = getIntent().getStringExtra(EXTRA_SCENE_ID);
        loadSceneConfig();
        viewPager.setAdapter(new SetupAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(pages.length);
    }

    private void loadSceneConfig() {
        sceneConfig = SceneConfig.loadFromPreferences(this, id);
    }

    @Override
    public void saveConnectableDevice(ConnectableDevice device) {
        if (device != null) {
            sceneConfig.devices.clear();
            String service = device.getServiceByName(WebOSTVService.ID) != null ? WebOSTVService.ID : DLNAService.ID;
            SceneConfig.DeviceConfig config = new SceneConfig.DeviceConfig(device.getFriendlyName(), service);
            sceneConfig.devices.add(config);
            sceneConfig.saveToPreferences(this, id);
        }
        viewPager.setCurrentItem(1, true);
    }

    @Override
    public void saveHueDevice(List<String> bulbs) {
        sceneConfig.bulbs = bulbs;
        viewPager.setCurrentItem(2, true);
        sceneConfig.saveToPreferences(this, id);
    }

    @Override
    public void saveWemoDevice(List<String> bulbs) {
        sceneConfig.wemos = bulbs;
        viewPager.setCurrentItem(3, true);
        sceneConfig.saveToPreferences(this, id);
    }

    @Override
    public void saveBeaconDevice(BeaconAdapter.ScannedBleDevice device) {
        if (device != null) {
            sceneConfig.beacon = device.MacAddress;
            sceneConfig.saveToPreferences(this, id);
        }
        viewPager.setCurrentItem(4, true);
    }

    @Override
    public void saveWinkDevice(List<String> bulbs) {
        sceneConfig.winkBulbs = bulbs;
        sceneConfig.saveToPreferences(this, id);
        finish();
    }

    private class SetupAdapter extends FragmentPagerAdapter {

        public SetupAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (pages[i] == null) {
                createPage(i);
            }
            return pages[i];
        }

        @Override
        public int getCount() {
            return pages.length;
        }
    }

    private void createPage(int i) {
        switch (i) {
            case 0:
                SceneConfig.DeviceConfig device = sceneConfig.devices.isEmpty()
                        ? new SceneConfig.DeviceConfig(null, null) : sceneConfig.devices.get(0);
                pages[0] = SetupMediaFragment.newInstance(device);
            case 1:
                pages[1] = SetupHueFragment.newInstance((ArrayList)sceneConfig.bulbs);
            case 2:
                pages[2] = SetupWeMoFragment.newInstance((ArrayList)sceneConfig.wemos);
            case 3:
                pages[3] = SetupBeaconFragment.newInstance(sceneConfig.beacon);
            case 4:
                pages[4] = SetupWinkFragment.newInstance((ArrayList)sceneConfig.winkBulbs);
        }
    }
}
