/*
 * SetupMediaFragment
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

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.CapabilityFilter;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.discovery.DiscoveryManagerListener;
import com.connectsdk.service.DLNAService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.smarthomesampler.scene.SceneConfig;

import java.util.Collection;
import java.util.List;

public class SetupMediaFragment extends SetupSingleChoiceFragment<ConnectableDevice> implements DiscoveryManagerListener {

    private SceneConfig.DeviceConfig selectedDevice;

    public interface ISaveConnectableDevice {
        public void saveConnectableDevice(ConnectableDevice device);
    }

    public static Fragment newInstance(SceneConfig.DeviceConfig device) {
        SetupMediaFragment fragment = new SetupMediaFragment();
        Bundle args = new Bundle();
        args.putSerializable("device", device);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedDevice = (SceneConfig.DeviceConfig)getArguments().getSerializable("device");
    }

    @Override
    void startDeviceDiscovery() {
        DiscoveryManager.getInstance().addListener(this);
        DiscoveryManager.getInstance().start();
        Collection<ConnectableDevice> devices = DiscoveryManager.getInstance().getAllDevices().values();
        for (ConnectableDevice connectableDevice : devices) {
            if (connectableDevice.getServiceByName(WebOSTVService.ID) != null
                    || connectableDevice.getServiceByName(DLNAService.ID) != null) {
                updateData(connectableDevice);
            }
        }
    }

    @Override
    void stopDeviceDiscovery() {
        DiscoveryManager.getInstance().removeListener(this);
        DiscoveryManager.getInstance().stop();
    }

    @Override
    void save(ConnectableDevice device) {
        ((ISaveConnectableDevice)getActivity()).saveConnectableDevice(device);
    }

    @Override
    String getDeviceName(ConnectableDevice device) {
        return device.getFriendlyName();
    }

    @Override
    String getTitleMessage() {
        return "Media devices";
    }

    @Override
    int getPosition() {
        for (int i = 0; i < adapter.getCount(); i++) {
            ConnectableDevice device = adapter.getItem(i);
            if (device.getFriendlyName().equals(selectedDevice.name)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onDeviceAdded(DiscoveryManager discoveryManager, ConnectableDevice connectableDevice) {
        if (connectableDevice.getServiceByName(WebOSTVService.ID) != null
            || connectableDevice.getServiceByName(DLNAService.ID) != null) {
            updateData(connectableDevice);
        }
    }

    @Override
    public void onDeviceUpdated(DiscoveryManager discoveryManager, ConnectableDevice connectableDevice) {
        if (connectableDevice.getServiceByName(WebOSTVService.ID) != null
                || connectableDevice.getServiceByName(DLNAService.ID) != null) {
            updateData(connectableDevice);
        }
    }

    @Override
    public void onDeviceRemoved(DiscoveryManager discoveryManager, ConnectableDevice connectableDevice) {
        if (adapter == null) {
            return;
        }
        adapter.remove(connectableDevice);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDiscoveryFailed(DiscoveryManager discoveryManager, ServiceCommandError serviceCommandError) {

    }

}
