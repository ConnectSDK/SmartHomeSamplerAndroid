/*
 * HueAdapter
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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import com.connectsdk.core.Util;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.hue.sdk.connection.impl.PHBridgeInternal;
import com.philips.lighting.hue.sdk.exception.PHHueException;
import com.philips.lighting.hue.sdk.utilities.PHUtilities;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.ArrayList;
import java.util.List;

public class HueAdapter implements PHSDKListener {

    private static final String APP_NAME = "SmartHomeSampler";
    private static final String TAG = "HueAdapter";
    private final PHHueSDK phHue;
    private PHBridge bridge;
    private final Context context;
    private final String name;
    private final List<HueListener> hueListeners = new ArrayList<HueListener>();
    private PHAccessPoint mActiveAccessPoint;

    public interface HueListener {
        public void onHueConnected();

        public void onHuePairingRequest(String name);
    }

    public HueAdapter(Context context) {
        phHue = PHHueSDK.getInstance();
        phHue.setAppName(APP_NAME);
        phHue.setDeviceName(Build.MODEL);
        this.context = context;
        this.name = "SmartHomeSamplerAndroid";
    }

    public void addConnectListener(HueListener listener) {
        hueListeners.add(listener);
    }

    public boolean isConnected() {
        return bridge != null;
    }

    public void startDiscovery() {
        phHue.getNotificationManager().registerSDKListener(this);
        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHue.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        sm.search(true, true);
    }

    public void stopDiscovery() {
        phHue.getNotificationManager().unregisterSDKListener(this);
        phHue.getNotificationManager().cancelSearchNotification();
    }


    @Override
    public void onCacheUpdated(List<Integer> integers, PHBridge phBridge) {

    }

    @Override
    public void onBridgeConnected(PHBridge phBridge) {
        phHue.setSelectedBridge(phBridge);
        phHue.enableHeartbeat(phBridge, PHHueSDK.HB_INTERVAL);
        bridge = phBridge;
        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                for (HueListener listener : hueListeners) {
                    listener.onHueConnected();
                }
            }
        });
    }

    @Override
    public void onAuthenticationRequired(final PHAccessPoint phAccessPoint) {
        phHue.startPushlinkAuthentication(phAccessPoint);
        if (hueListeners != null) {
            Util.runOnUI(new Runnable() {
                @Override
                public void run() {
                    for (HueListener listener : hueListeners) {
                        listener.onHuePairingRequest("PhilipsHue (" + phAccessPoint.getIpAddress() + ")");
                    }
                }
            });
        }
    }

    @Override
    public void onAccessPointsFound(List<PHAccessPoint> phAccessPoints) {
        if (phAccessPoints != null && phAccessPoints.size() > 0) {
            phHue.getAccessPointsFound().clear();
            phHue.getAccessPointsFound().addAll(phAccessPoints);

            for (final PHAccessPoint phAccessPoint : phAccessPoints) {
                phAccessPoint.setUsername(getUsername());
                connect(phAccessPoint);
            }
        }
    }

    private String getUsername() {
        SharedPreferences pref = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        String username = pref.getString("hue", "");
        if ("".equals(username)) {
            username = PHBridgeInternal.generateUniqueKey();
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("hue", username);
            editor.apply();
        }
        return username;
    }

    @Override
    public void onError(int i, String s) {
        Log.e(TAG, "Error: " + s);
    }

    @Override
    public void onConnectionResumed(PHBridge phBridge) {
        bridge = phBridge;
        phHue.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
        for (int i = 0; i < phHue.getDisconnectedAccessPoint().size(); i++) {

            if (phHue.getDisconnectedAccessPoint().get(i).getIpAddress().equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
                phHue.getDisconnectedAccessPoint().remove(i);
            }
        }

        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                for (HueListener listener : hueListeners) {
                    listener.onHueConnected();
                }
            }
        });
    }

    @Override
    public void onConnectionLost(PHAccessPoint phAccessPoint) {
        if (!phHue.getDisconnectedAccessPoint().contains(phAccessPoint)) {
            phHue.getDisconnectedAccessPoint().add(phAccessPoint);
        }
    }

    @Override
    public void onParsingErrors(List<PHHueParsingError> phHueParsingErrors) {
        Log.e(TAG, "Error: onParsingErrors");
    }


    void connect(PHAccessPoint accessPoint) {
        try {
            mActiveAccessPoint = accessPoint;
            phHue.connect(accessPoint);
        } catch (PHHueException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        phHue.getNotificationManager().unregisterSDKListener(this);
        if (bridge != null) {
            phHue.disableAllHeartbeat();
            try {
                phHue.disconnect(bridge);
            } catch (Exception e) {
                e.printStackTrace();
            }
            bridge = null;
        }
    }

    public List<PHLight> getAllBulbs() {
        if (bridge == null) {
            return null;
        }
        return bridge.getResourceCache().getAllLights();
    }

    public boolean setColor(PHLight light, int color) {
        if (bridge == null) {
            return false;
        }

        float[] xy = PHUtilities.calculateXYFromRGB(Color.red(color), Color.green(color), Color.blue(color), light.getModelNumber());
        PHLightState state = new PHLightState();
        state.setX(xy[0]);
        state.setY(xy[1]);
        state.setEffectMode(PHLight.PHLightEffectMode.EFFECT_NONE);
        state.setAlertMode(PHLight.PHLightAlertMode.ALERT_NONE);
        bridge.updateLightState(light, state);
        return true;
    }

    public boolean setBrightness(PHLight light, int brightness) {
        if (bridge == null) {
            return false;
        }

        PHLightState state = new PHLightState();
        state.setBrightness(brightness);
        state.setEffectMode(PHLight.PHLightEffectMode.EFFECT_NONE);
        state.setAlertMode(PHLight.PHLightAlertMode.ALERT_NONE);
        bridge.updateLightState(light, state);
        return true;
    }

    public boolean setPower(PHLight light, boolean power) {
        if (bridge == null) {
            return false;
        }

        PHLightState state = new PHLightState();
        state.setOn(power);
        state.setEffectMode(PHLight.PHLightEffectMode.EFFECT_NONE);
        state.setAlertMode(PHLight.PHLightAlertMode.ALERT_NONE);
        bridge.updateLightState(light, state);
        return true;
    }

}
