/*
 * WeMoAdapter
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

import android.util.Log;

import com.belkin.wemo.localsdk.WeMoDevice;
import com.belkin.wemo.localsdk.WeMoSDKContext;
import com.connectsdk.core.Util;
import com.connectsdk.smarthomesampler.scene.SceneConfig;

import java.util.ArrayList;
import java.util.List;

public class WeMoAdapter implements WeMoSDKContext.NotificationListener {


    private final WeMoSDKContext wemoContext;
    private final SceneConfig config;
    private final List<WeMoDevice> wemoDevices = new ArrayList<WeMoDevice>();
    private final List<ConnectListener> connectListeners = new ArrayList<ConnectListener>();

    public interface ConnectListener {
        void onWeMoConnected();
    }

    public WeMoAdapter(WeMoSDKContext weMoContext, SceneConfig config) {
        this.wemoContext = weMoContext;
        this.config = config;
        wemoContext.addNotificationListener(this);
    }

    public void addConnectListener(ConnectListener listener) {
        connectListeners.add(listener);
    }

    @Override
    public void onNotify(String event, String udn) {
        WeMoDevice device = wemoContext.getWeMoDeviceByUDN(udn);
        if (device != null && !wemoDevices.contains(device) && config.wemos.contains(udn)) {
            wemoDevices.add(device);
        }
        if (isConnected()) {
            Util.runOnUI(new Runnable() {
                @Override
                public void run() {
                    for (ConnectListener listener : connectListeners) {
                        listener.onWeMoConnected();
                    }
                }
            });
        }
    }
    
    public void enableDevices(String state) {
        for (WeMoDevice device : wemoDevices) {
            wemoContext.setDeviceState(state, device.getUDN());
        }
    }
    
    public void stop() {
        wemoContext.removeNotificationListener(this);
    }

    public boolean isConnected() {
        return config.wemos.size() == wemoDevices.size();
    }
}
