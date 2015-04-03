/*
 * SceneConfig
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
package com.connectsdk.smarthomesampler.scene;

import android.content.Context;
import android.content.SharedPreferences;

import com.connectsdk.device.ConnectableDevice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SceneConfig {

    public final List<DeviceConfig> devices;
    
    public List<String> bulbs;
    
    public List<String> wemos;
    
    public List<String> winkBulbs;

    public final String sceneName;

    public String beacon;

    public SceneConfig(String name, List<DeviceConfig> devices, List<String> bulbs, List<String> wemos, List<String> winkBulbs) {
        this.sceneName = name;
        this.devices = devices;
        this.bulbs = bulbs;
        this.wemos = wemos;
        this.winkBulbs = winkBulbs;
    }

    public static SceneConfig loadFromPreferences(Context context, String id) {
        SharedPreferences pref = context.getSharedPreferences(id, Context.MODE_PRIVATE);

        List<DeviceConfig> devices = new ArrayList<>();
        List<String> bulbs = new ArrayList<>();
        List<String> wemos = new ArrayList<>();
        List<String> winkBulbs = new ArrayList<>();

        Set<String> mediaDevices = pref.getStringSet("mediaDevices", null);
        if (mediaDevices != null) {
            for (String mediaDevice : mediaDevices) {
                String[] parts = mediaDevice.split("#");
                DeviceConfig device = new DeviceConfig(parts[0], parts[1]);
                devices.add(device);
            }
        }

        Set<String> bulbSet = pref.getStringSet("bulbs", null);
        if (bulbSet != null) {
            for (String item : bulbSet) {
                bulbs.add(item);
            }
        }

        Set<String> wemoSet = pref.getStringSet("wemos", null);
        if (wemoSet != null) {
            for (String item : wemoSet) {
                wemos.add(item);
            }
        }

        Set<String> winkSet = pref.getStringSet("winks", null);
        if (winkSet != null) {
            for (String item : winkSet) {
                winkBulbs.add(item);
            }
        }

        SceneConfig config = new SceneConfig(id, devices, bulbs, wemos, winkBulbs);
        config.beacon = pref.getString("ibeacon", "");
        return config;
    }

    public void saveToPreferences(Context context, String id) {
        SharedPreferences.Editor editor = context.getSharedPreferences(id, Context.MODE_PRIVATE).edit();

        Set<String> mediaDevices = new HashSet<String>();
        for (DeviceConfig config : devices) {
            mediaDevices.add(config.name + "#" + config.requiredService);
        }
        editor.putStringSet("mediaDevices", mediaDevices);

        Set<String> bulbsSet = new HashSet<String>();
        for (String item : bulbs) {
            bulbsSet.add(item);
        }
        editor.putStringSet("bulbs", bulbsSet);

        Set<String> wemoSet = new HashSet<String>();
        for (String item : wemos) {
            wemoSet.add(item);
        }
        editor.putStringSet("wemos", wemoSet);

        Set<String> winkSet = new HashSet<String>();
        for (String item : winkBulbs) {
            winkSet.add(item);
        }
        editor.putStringSet("winks", winkSet);
        editor.putString("ibeacon", beacon);
        editor.apply();
    }

    public boolean isConfigured() {
        return !devices.isEmpty() && (!bulbs.isEmpty() || !winkBulbs.isEmpty());
    }

    public static class DeviceConfig implements Serializable {

        public final String name;
        
        public final String requiredService;
        
        public DeviceConfig(String name, String service) {
            this.name = name;
            this.requiredService = service;
        }

        @Override
        public String toString() {
            return name + "#" + requiredService;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DeviceConfig)) return false;

            DeviceConfig that = (DeviceConfig) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }

    public static String getServiceName(SceneConfig config, ConnectableDevice d) {
        for (SceneConfig.DeviceConfig dc : config.devices) {
            if (d.getFriendlyName().contains(dc.name)) {
                return dc.requiredService;
            }
        }
        return "";
    }


    public static boolean contains(SceneConfig config, ConnectableDevice d) {
        for (SceneConfig.DeviceConfig dc : config.devices) {
            if (d.getFriendlyName().contains(dc.name)) {
                return true;
            }
        }
        return false;
    }
}
