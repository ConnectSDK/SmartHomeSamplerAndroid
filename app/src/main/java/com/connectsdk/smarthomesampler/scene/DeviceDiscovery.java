/*
 * DeviceDiscovery
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

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.command.ServiceCommandError;

import java.util.HashMap;
import java.util.Map;

class DeviceDiscovery implements DiscoveryManagerListener {

    private final SceneConfig config;
    private final Map<String, ConnectableDevice> foundDevices = new HashMap<String, ConnectableDevice>();
    private volatile boolean ready;

    public interface DeviceDiscovered {

        public void onDevicesReadyToConnect(Map<String, ConnectableDevice> devices);

    }

    private final DeviceDiscovered listener;

    public DeviceDiscovery(SceneConfig config, DeviceDiscovered listener) {
        this.config = config;
        this.listener = listener;
    }

    public void start() {
        addFoundDevices();
        DiscoveryManager.getInstance().addListener(this);
        DiscoveryManager.getInstance().stop();
        DiscoveryManager.getInstance().start();
    }

    private void addFoundDevices() {
        for (ConnectableDevice d : DiscoveryManager.getInstance().getAllDevices().values()) {
            onDeviceAdded(DiscoveryManager.getInstance(), d);
        }
        checkDevices();
    }

    public void stop() {
        DiscoveryManager.getInstance().removeListener(this);
    }

    private synchronized void checkDevices() {
        if (foundDevices.size() == config.devices.size()) {
            for (ConnectableDevice d : foundDevices.values()) {
                DeviceService s = d.getServiceByName(SceneConfig.getServiceName(config, d));
                if (s == null) {
                    return;
                }
            }

            if (!ready && listener != null) {
                ready = true;
                listener.onDevicesReadyToConnect(foundDevices);
                stop();
            }
        }
    }

    @Override
    public void onDeviceAdded(DiscoveryManager manager, ConnectableDevice device) {
        for (SceneConfig.DeviceConfig dc : config.devices) {
            if (device.getFriendlyName().contains(dc.name)) {
                foundDevices.put(device.getIpAddress(), device);
            }            
        }
        checkDevices();
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager manager, ConnectableDevice device) {
        for (SceneConfig.DeviceConfig dc : config.devices) {
            if (device.getFriendlyName().contains(dc.name)) {
                foundDevices.put(device.getIpAddress(), device);
            }
        }
        checkDevices();
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager manager, ConnectableDevice device) {

    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager manager, ServiceCommandError error) {

    }
}
