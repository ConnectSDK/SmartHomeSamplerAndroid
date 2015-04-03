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
import android.util.Log;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.DLNAService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.smarthomesampler.adapter.BeaconAdapter;
import com.connectsdk.smarthomesampler.dialog.MessageFragmentDialog;
import com.connectsdk.smarthomesampler.fragment.SetupBeaconFragment;
import com.connectsdk.smarthomesampler.fragment.SetupHueFragment;
import com.connectsdk.smarthomesampler.fragment.SetupMediaFragment;
import com.connectsdk.smarthomesampler.fragment.SetupWeMoFragment;
import com.connectsdk.smarthomesampler.fragment.SetupWinkFragment;
import com.connectsdk.smarthomesampler.scene.SceneConfig;
import com.connectsdk.smarthomesampler.scene.SceneController;

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

    private final Fragment[] pages = new Fragment[5];

    private SceneConfig sceneConfig;

    private SceneConfig anotherSceneConfig;

    private String id;

    private String anotherSceneId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_setup);
        ButterKnife.inject(this);
        id = getIntent().getStringExtra(EXTRA_SCENE_ID);
        if (SceneController.SCENE_ID_FIRST.equals(id)) {
            anotherSceneId = SceneController.SCENE_ID_SECOND;
        } else {
            anotherSceneId = SceneController.SCENE_ID_FIRST;
        }
        loadSceneConfig();
        viewPager.setAdapter(new SetupAdapter(getSupportFragmentManager()));
        viewPager.setOffscreenPageLimit(pages.length);
        getSupportActionBar().setTitle(getString(R.string.configure) + " " + id);
    }

    private void loadSceneConfig() {
        sceneConfig = SceneConfig.loadFromPreferences(this, id);
        anotherSceneConfig = SceneConfig.loadFromPreferences(this, anotherSceneId);
    }

    @Override
    public boolean saveConnectableDevice(ConnectableDevice device) {
        if (device != null) {
            sceneConfig.devices.clear();
            String service = device.getServiceByName(WebOSTVService.ID) != null ? WebOSTVService.ID : DLNAService.ID;
            SceneConfig.DeviceConfig config = new SceneConfig.DeviceConfig(device.getFriendlyName(), service);
            // check another scene
            if (anotherSceneConfig.devices.contains(config)) {
                return false;
            }
            sceneConfig.devices.add(config);
            sceneConfig.saveToPreferences(this, id);
        }
        viewPager.setCurrentItem(1, true);
        return true;
    }

    @Override
    public void forceSaveConnectableDevice(ConnectableDevice device) {
        if (device != null) {
            SceneConfig.DeviceConfig config = new SceneConfig.DeviceConfig(device.getFriendlyName(), null);
            anotherSceneConfig.devices.remove(config);
            anotherSceneConfig.saveToPreferences(this, anotherSceneId);
        }
        saveConnectableDevice(device);
    }

    @Override
    public boolean saveHueDevice(List<String> bulbs) {
        for (String bulb : bulbs) {
            if (anotherSceneConfig.bulbs.contains(bulb)) {
                return false;
            }
        }
        sceneConfig.bulbs = bulbs;
        viewPager.setCurrentItem(2, true);
        sceneConfig.saveToPreferences(this, id);
        return true;
    }

    @Override
    public void forceSaveHueDevice(List<String> bulbs) {
        anotherSceneConfig.bulbs.removeAll(bulbs);
        anotherSceneConfig.saveToPreferences(this, anotherSceneId);
        saveHueDevice(bulbs);
    }

    @Override
    public boolean saveWemoDevice(List<String> bulbs) {
        for (String bulb : bulbs) {
            if (anotherSceneConfig.wemos.contains(bulb)) {
                return false;
            }
        }
        sceneConfig.wemos = bulbs;
        viewPager.setCurrentItem(3, true);
        sceneConfig.saveToPreferences(this, id);
        return true;
    }

    @Override
    public void forceSaveWemoDevice(List<String> bulbs) {
        anotherSceneConfig.wemos.removeAll(bulbs);
        anotherSceneConfig.saveToPreferences(this, anotherSceneId);
        saveWemoDevice(bulbs);
    }

    @Override
    public boolean saveBeaconDevice(BeaconAdapter.ScannedBleDevice device) {
        if (device != null && device.macAddress != null && device.macAddress.equals(anotherSceneConfig.beacon)) {
            return false;
        }
        if (device != null) {
            sceneConfig.beacon = device.macAddress;
            sceneConfig.saveToPreferences(this, id);
        }
        viewPager.setCurrentItem(4, true);
        return true;
    }

    @Override
    public void forceSaveBeaconDevice(BeaconAdapter.ScannedBleDevice device) {
        anotherSceneConfig.beacon = "";
        anotherSceneConfig.saveToPreferences(this, anotherSceneId);
        saveBeaconDevice(device);
    }

    @Override
    public boolean saveWinkDevice(List<String> bulbs) {
        for (String bulb : bulbs) {
            if (anotherSceneConfig.winkBulbs.contains(bulb)) {
                return false;
            }
        }
        sceneConfig.winkBulbs = bulbs;
        sceneConfig.saveToPreferences(this, id);
        if (sceneConfig.isConfigured()) {
            finish();
        } else {
            MessageFragmentDialog.newInstance("Scene is not configured", "Please select one media device and at least one bulb to finish configuration.").show(getSupportFragmentManager(), "msg_dialog");
        }
        return true;
    }

    @Override
    public void forceSaveWinkDevice(List<String> bulbs) {
        anotherSceneConfig.winkBulbs.removeAll(bulbs);
        anotherSceneConfig.saveToPreferences(this, anotherSceneId);
        saveWinkDevice(bulbs);
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

    @SuppressWarnings("unchecked")
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
