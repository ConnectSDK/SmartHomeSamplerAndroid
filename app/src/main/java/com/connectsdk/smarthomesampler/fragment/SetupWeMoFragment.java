/*
 * SetupWeMoFragment
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
import android.content.DialogInterface;
import android.support.v4.app.Fragment;

import com.belkin.wemo.localsdk.WeMoDevice;
import com.belkin.wemo.localsdk.WeMoSDKContext;
import com.connectsdk.smarthomesampler.R;
import com.connectsdk.smarthomesampler.dialog.ConfirmationFragmentDialog;

import java.util.ArrayList;
import java.util.List;

public class SetupWeMoFragment extends SetupMultiChoiceFragment<WeMoDevice> implements WeMoSDKContext.NotificationListener {

    private WeMoSDKContext wemoContext;

    public interface ISaveWemoDevice {
        public boolean saveWemoDevice(List<String> bulbs);

        public void forceSaveWemoDevice(List<String> bulbs);
    }

    public static Fragment newInstance(ArrayList<String> bulbs) {
        SetupWeMoFragment fragment = new SetupWeMoFragment();
        return SetupMultiChoiceFragment.setArguments(fragment, bulbs);
    }

    @Override
    void startDeviceDiscovery() {
        wemoContext.addNotificationListener(this);
        wemoContext.refreshListOfWeMoDevicesOnLAN();
    }

    @Override
    void stopDeviceDiscovery() {
        wemoContext.removeNotificationListener(this);
        wemoContext.stop();
    }

    @Override
    void save() {
        final ISaveWemoDevice activity = ((ISaveWemoDevice) getActivity());
        if (!activity.saveWemoDevice(ids)) {
            ConfirmationFragmentDialog dlg = ConfirmationFragmentDialog.newInstance(getString(R.string.warning), getString(R.string.device_will_be_removed_from_scene));
            dlg.setPositiveListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.forceSaveWemoDevice(ids);
                }
            });
            dlg.show(getActivity().getSupportFragmentManager(), "");
        }
    }

    @Override
    String getDeviceId(WeMoDevice device) {
        return device.getUDN();
    }

    @Override
    String getDeviceName(WeMoDevice device) {
        return device.getFriendlyName();
    }

    @Override
    String getTitleMessage() {
        return "WeMo devices";
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        wemoContext = new WeMoSDKContext(getActivity().getApplicationContext());
    }

    @Override
    public void onNotify(String event, String udn) {
        final WeMoDevice device = wemoContext.getWeMoDeviceByUDN(udn);
        if (device != null) {
            updateData(device);
        }
    }

}
