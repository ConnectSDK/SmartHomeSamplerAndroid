/*
 * Scene
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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.graphics.Palette;
import android.util.Log;

import com.belkin.wemo.localsdk.WeMoDevice;
import com.belkin.wemo.localsdk.WeMoSDKContext;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.device.ConnectableDeviceListener;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.smarthomesampler.IUserNotificationListener;
import com.connectsdk.smarthomesampler.adapter.HueAdapter;
import com.connectsdk.smarthomesampler.adapter.WeMoAdapter;
import com.connectsdk.smarthomesampler.adapter.WinkAdapter;
import com.philips.lighting.model.PHLight;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Scene implements IScene, ConnectableDeviceListener, DeviceDiscovery.DeviceDiscovered,
        HueAdapter.HueListener, WeMoAdapter.ConnectListener {
    private static final long TIMER_UPDATE_DELAY = 1000;
    private static final long TIMER_COLOR_DELAY = 10000;
    private static final long DELAY_BETWEEN_TRACKS = 1000;
    private static final long WAKE_UP_DELAY = 5000;
    private static final float MAX_VOLUME = 0.11f;
    private static MediaServer server;

    private final String name;
    private final SceneConfig config;
    private Map<String, ConnectableDevice> connectableDevices = new HashMap<>();
    private SceneInfo sceneInfo;
    private final IUserNotificationListener listener;
    private WinkAdapter winkAdapter;
    private WeMoAdapter weMoAdapter;
    private HueAdapter hueAdapter;
    private SceneState mState = SceneState.Connecting;
    private LaunchSession mediaSession;
    private Timer durationTimer;
    private boolean next;
    private volatile Timer wakeUpTimer;
    private Timer smoothTimer;
    private int brightness;
    private float volume;
    private Timer volumeTimer;

    static enum SceneState {
        Connecting,
        Connected,
        PlayAfterConnect,
        Play,
        WakeUp,
        Disconnect,
        Stop,
        Fullstop, PlaySmoothly,
    }


    private final int[] colors = new int[4];
    private Timer colorTimer;
    private int currentColor;

    private final Target onBitmapLoaded = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Palette palette = Palette.generate(bitmap);
            colors[0] = palette.getVibrantColor(0);
            colors[1] = palette.getDarkVibrantColor(0);
            colors[2] = palette.getLightVibrantColor(0);
            colors[3] = palette.getMutedColor(0);
            startColorTimer();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };


    public Scene(String name, SceneConfig config, IUserNotificationListener listener) {
        this.name = name;
        this.config = config;
        this.listener = listener;
        DeviceDiscovery discovery = new DeviceDiscovery(config, this);
        discovery.start();
        mState = SceneState.Connected;
    }

    @Override
    public void setHueAdapter(HueAdapter hueAdapter) {
        this.hueAdapter = hueAdapter;
        hueAdapter.addConnectListener(this);
        if (hueAdapter.isConnected()) {
            onDeviceReady(null);
        }
    }

    @Override
    public void setWeMoContext(WeMoSDKContext weMoContext) {
        this.weMoAdapter = new WeMoAdapter(weMoContext, config);
        if (weMoAdapter.isConnected()) {
            onDeviceReady(null);
        }
    }

    public void setWinkAdapter(WinkAdapter winkAdapter) {
        this.winkAdapter = winkAdapter;
    }

    @Override
    public void setSceneInfo(SceneInfo sceneInfo) {
        this.sceneInfo = sceneInfo;
    }

    @Override
    public SceneInfo getSceneInfo() {
        return sceneInfo;
    }

    @Override
    public void setState(IScene.State state) {
        switch (state) {
            case Play:
                if (mState.equals(SceneState.Connecting)) {
                    mState = SceneState.PlayAfterConnect;
                } else {
                    play(getCurrentTrack(), DELAY_BETWEEN_TRACKS, true);
                }
                break;
            case Stop:
                stop(true);
                break;
            case Smoothstop:
                stopMediaSmoothly();
                mState = SceneState.WakeUp;
                break;
            case Fullstop:
                stop(false);
                mState = SceneState.Fullstop;
                break;
            case WakeUp:
                wakeUpTimer();
                break;
        }
    }

    @Override
    public State getState() {
        if (mState.equals(SceneState.WakeUp) || mState.equals(SceneState.PlaySmoothly))
            return State.WakeUp;
        else if (mState.equals(SceneState.Stop) || mState.equals(SceneState.Fullstop))
            return State.Stop;

        return State.Play;
    }

    @Override
    public boolean isConnected() {
        return mState != SceneState.Connecting && mState != SceneState.Disconnect;
    }

    @Override
    public SceneConfig getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return name;
    }


    // internal methods 

    @Override
    public void onDevicesReadyToConnect(Map<String, ConnectableDevice> devices) {
        connectableDevices = devices;
        for (ConnectableDevice d : connectableDevices.values()) {
            if (!d.isConnected() && d.isConnectable()) {
                d.addListener(this);
                d.connect();
            } else {
                onDeviceReady(d);
            }
        }
    }

    synchronized void stop(boolean disableBluetooth) {
        mState = SceneState.Stop;
        if (disableBluetooth) {
            listener.onRequestBluetoothStop();
        }
        stopColorTimer();
        stopTimeTracking();
        stopWakeUpTimer();

        if (server != null) {
            server.stop();
        }

        if (mediaSession != null) {
            mediaSession.close(null);
            mediaSession = null;
        }

        weMoAdapter.enableDevices(WeMoDevice.WEMO_DEVICE_OFF);
        enableWinkDevices(false);

        if (hueAdapter != null) {
            List<PHLight> bulbs = hueAdapter.getAllBulbs();
            if (bulbs != null) {
                for (PHLight bulb : bulbs) {
                    if (config.bulbs.contains(bulb.getIdentifier())) {
                        hueAdapter.setPower(bulb, false);
                    }
                }
            }
            hueAdapter.stopDiscovery();
        }
    }

    private void stopWakeUpTimer() {
        if (wakeUpTimer != null) {
            wakeUpTimer.cancel();
            wakeUpTimer.purge();
            wakeUpTimer = null;
        }
    }

    private void enableWinkDevices(final boolean enable) {
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                for (String id : config.winkBulbs) {
                    try {
                        winkAdapter.updateBulb(id, enable, 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    void play(final MediaInfo mediaInfo, final long delay, boolean allDevices) {
        if (!mState.equals(SceneState.Connected) && !mState.equals(SceneState.Stop)
                && !mState.equals(SceneState.PlaySmoothly)) {
            return;
        }
        if (!mState.equals(SceneState.PlaySmoothly)) {
            mState = SceneState.Play;
        }
        if (!next && mediaSession != null) {
            return;
        }

        if (allDevices) {
            // load image and set lamp color
            List<ImageInfo> images = mediaInfo.getImages();
            Picasso.with(DiscoveryManager.getInstance().getContext())
                    .load(images.get(0).getUrl()).into(onBitmapLoaded);

            enableWinkDevices(true);
        }

        // enable devices
        weMoAdapter.enableDevices(WeMoDevice.WEMO_DEVICE_ON);

        setVolume(MAX_VOLUME);
        // play media on the devices
        playMedia(mediaInfo, delay);
    }


    private void playMedia(MediaInfo mediaInfo, final long delay) {
        for (final ConnectableDevice device : connectableDevices.values()) {

            MediaPlayer player = device.getCapability(MediaPlayer.class);

            if (player != null) {
                player.playMedia(mediaInfo, false, new MediaPlayer.LaunchListener() {

                    public void onSuccess(final MediaPlayer.MediaLaunchObject object) {
                        mediaSession = object.launchSession;
                        Log.e("", "scene playMedia OK ");
                        seek(object, device, delay);

                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Log.e("", "scene playMedia error " + error.getCode() + " " + error.getMessage());
                    }
                });
            }
        }
    }

    private void playMediaSmoothly(MediaInfo mediaInfo, final long delay) {
        mState = SceneState.Play;

        // load image and set lamp color
        List<ImageInfo> images = mediaInfo.getImages();
        Picasso.with(DiscoveryManager.getInstance().getContext())
                .load(images.get(0).getUrl()).into(onBitmapLoaded);

        for (final ConnectableDevice device : connectableDevices.values()) {

            MediaPlayer player = device.getCapability(MediaPlayer.class);

            if (player != null) {
                player.playMedia(mediaInfo, false, new MediaPlayer.LaunchListener() {

                    public void onSuccess(final MediaPlayer.MediaLaunchObject object) {
                        mediaSession = object.launchSession;
                        Log.e("", "scene playMedia OK ");
                        seek(object, device, delay);
                        startSmoothTimer(10, 80, true, 10);
                        startVolumeTimer(0.01f, MAX_VOLUME, true, 0.01f, null);
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Log.e("", "scene playMedia error " + error.getCode() + " " + error.getMessage());
                    }
                });
            }
        }
    }

    private void stopMediaSmoothly() {
        stopColorTimer();
        startSmoothTimer(80, 10, false, -10);
        startVolumeTimer(MAX_VOLUME, 0.01f, false, -0.01f, new Runnable() {
            @Override
            public void run() {
                stop(true);
                wakeUpTimer();
            }
        });
    }

    private void seek(final MediaPlayer.MediaLaunchObject object, ConnectableDevice device, final long delay) {
        if (sceneInfo.position > 0) {
            if (SceneConfig.getServiceName(config, device).equals(WebOSTVService.ID)) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        seekAndStartTimer(object, delay);
                    }
                }, 1000);
            } else {
                seekAndStartTimer(object, delay);
            }

        } else {
            startTimeTracking(object.mediaControl, delay);
        }
    }

    private void seekAndStartTimer(final MediaPlayer.MediaLaunchObject object, final long delay) {
        object.mediaControl.seek(sceneInfo.position, new ResponseListener<Object>() {
            @Override
            public void onSuccess(Object o) {
                startTimeTracking(object.mediaControl, delay);
            }

            @Override
            public void onError(ServiceCommandError serviceCommandError) {
                Log.e("", "scene seek failed");
            }
        });
    }


    @Override
    public void onDeviceReady(ConnectableDevice connectableDevice) {
        if (hueAdapter == null || weMoAdapter == null) {
            return;
        }
        for (ConnectableDevice device : connectableDevices.values()) {
            if (!device.isConnected() && device.isConnectable()) {
                return;
            }
        }
        if (!config.bulbs.isEmpty() && !hueAdapter.isConnected()) {
            return;
        }
        if (!config.wemos.isEmpty() && !weMoAdapter.isConnected()) {
            return;
        }
        // OK
        if (mState.equals(SceneState.PlayAfterConnect)) {
            mState = SceneState.Connected;
            play(getCurrentTrack(), DELAY_BETWEEN_TRACKS, true);
        } else {
            mState = SceneState.Connected;
        }
        if (listener != null) {
            listener.onSceneConnected(name);
        }
    }

    @Override
    public void onDeviceDisconnected(ConnectableDevice connectableDevice) {

    }

    @Override
    public void onPairingRequired(final ConnectableDevice connectableDevice,
                                  DeviceService deviceService, DeviceService.PairingType pairingType) {
        if (listener != null && SceneConfig.contains(config, connectableDevice)) {
            Util.runOnUI(new Runnable() {
                @Override
                public void run() {
                    listener.onPairingRequest(connectableDevice.getFriendlyName());
                }
            });
        }
    }

    @Override
    public void onCapabilityUpdated(ConnectableDevice connectableDevice, List<String> strings, List<String> strings2) {

    }

    @Override
    public void onConnectionFailed(ConnectableDevice connectableDevice, ServiceCommandError serviceCommandError) {

    }


    @Override
    public void onHueConnected() {
        onDeviceReady(null);
    }

    @Override
    public void onHuePairingRequest(final String name) {
        if (listener != null) {
            Util.runOnUI(new Runnable() {
                @Override
                public void run() {
                    listener.onPairingRequest(name);
                }
            });
        }
    }

    @Override
    public void onWeMoConnected() {
        onDeviceReady(null);
    }

    private void startColorTimer() {
        stopColorTimer();
        colorTimer = new Timer();
        colorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentColor > 3) {
                    currentColor = 0;
                }
                final int color = colors[currentColor];
                ++currentColor;
                if (hueAdapter != null) {
                    List<PHLight> bulbs = hueAdapter.getAllBulbs();
                    if (bulbs != null) {
                        for (PHLight bulb : bulbs) {
                            if (config.bulbs.contains(bulb.getIdentifier())) {
                                hueAdapter.setPower(bulb, true);
                                hueAdapter.setColor(bulb, color);
                            }
                        }
                    }
                }
            }
        }, 0, TIMER_COLOR_DELAY);
    }

    private void stopColorTimer() {
        if (colorTimer != null) {
            colorTimer.cancel();
            colorTimer.purge();
            colorTimer = null;
        }
    }


    private void startSmoothTimer(final int start, final int end, final boolean greater, final int increment) {
        brightness = start;
        stopSmoothTimer();
        enableLights(true);
        smoothTimer = new Timer();
        smoothTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                listener.onRequestBluetoothStop();
                // Philips bulbs
                if (hueAdapter != null) {
                    List<PHLight> bulbs = hueAdapter.getAllBulbs();
                    if (bulbs != null) {
                        for (PHLight bulb : bulbs) {
                            if (config.bulbs.contains(bulb.getIdentifier())) {
                                hueAdapter.setBrightness(bulb, brightness);
                            }
                        }
                    }
                    brightness += increment;
                    if (greater) {
                        if (brightness >= end) {
                            stopSmoothTimer();
                            enableWinkDevices(true);
                        }
                    } else {
                        if (brightness <= end) {
                            stopSmoothTimer();
                            enableWinkDevices(false);
                            enableLights(false);
                        }
                    }
                }

            }
        }, 0, 400);
    }

    private void enableLights(final boolean enable) {
        if (hueAdapter != null) {
            List<PHLight> bulbs = hueAdapter.getAllBulbs();
            if (bulbs != null) {
                for (PHLight bulb : bulbs) {
                    if (config.bulbs.contains(bulb.getIdentifier())) {
                        hueAdapter.setPower(bulb, enable);
                        hueAdapter.setColor(bulb, Color.WHITE);
                    }
                }
            }
        }
    }

    private void stopSmoothTimer() {
        if (smoothTimer != null) {
            smoothTimer.cancel();
            smoothTimer.purge();
            smoothTimer = null;
        }
    }

    private void startVolumeTimer(final float start, final float end, final boolean condGreater,
                                  final float incement, final Runnable runnable) {
        volume = start;
        stopVolumeTimer();
        volumeTimer = new Timer();
        volumeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                listener.onRequestBluetoothStop();
                for (final ConnectableDevice device : connectableDevices.values()) {
                    VolumeControl volumeControl = device.getCapability(VolumeControl.class);
                    if (volumeControl != null) {
                        volumeControl.setVolume(volume, new ResponseListener<Object>() {
                            @Override
                            public void onSuccess(Object object) {
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                                Log.e("", "scene volume set Error " + error.getMessage());
                            }
                        });
                        volume += incement;
                        if (condGreater) {
                            if (volume >= end) {
                                stopVolumeTimer();
                                if (runnable != null)
                                    runnable.run();
                            }
                        } else {
                            if (volume <= end) {
                                stopVolumeTimer();
                                if (runnable != null)
                                    runnable.run();
                            }
                        }

                    }
                }
            }
        }, 0, 400);
    }


    private void stopVolumeTimer() {
        if (volumeTimer != null) {
            volumeTimer.cancel();
            volumeTimer.purge();
            volumeTimer = null;
        }
    }

    private void setVolume(float currentVolume) {
        if (currentVolume > 0.15f) {
            return;
        }
        for (final ConnectableDevice device : connectableDevices.values()) {
            VolumeControl volumeControl = device.getCapability(VolumeControl.class);
            if (volumeControl != null && SceneConfig.contains(config, device)) {
                volumeControl.setVolume(currentVolume, new ResponseListener<Object>() {
                    @Override
                    public void onSuccess(Object object) {
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Log.e("", "scene volume error " + error.getMessage());
                    }
                });
            }
        }
    }


    private synchronized void startTimeTracking(final MediaControl mediaControl, final long delay) {
        stopTimeTracking();
        durationTimer = new Timer();
        durationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getTotalDuration(mediaControl, delay);
            }
        }, TIMER_UPDATE_DELAY);
    }

    private void getTotalDuration(final MediaControl mediaControl, final long delay) {
        mediaControl.getDuration(new MediaControl.DurationListener() {
            @Override
            public void onSuccess(final Long totalDuration) {
                if (totalDuration > 0) {
                    durationTimer = new Timer();
                    durationTimer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            updateTrackPosition(totalDuration, delay);
                        }
                    }, TIMER_UPDATE_DELAY, TIMER_UPDATE_DELAY);
                } else {
                    durationTimer = new Timer();
                    durationTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            getTotalDuration(mediaControl, delay);
                        }
                    }, 200);
                }
            }

            @Override
            public void onError(ServiceCommandError serviceCommandError) {
                Log.e("", "scene track duration failed ");
            }
        });
    }

    private void updateTrackPosition(Long totalDuration, long delay) {
        sceneInfo.position += TIMER_UPDATE_DELAY;
        if (totalDuration <= sceneInfo.position) {
            next(delay);
        }
    }

    private synchronized void stopTimeTracking() {
        if (durationTimer != null) {
            durationTimer.cancel();
            durationTimer.purge();
            durationTimer = null;
        }
    }

    private synchronized void next(long delay) {
        stopColorTimer();
        stopTimeTracking();
        sceneInfo.position = 0;
        if (mState.equals(SceneState.PlaySmoothly)) {
            sceneInfo.playListIndex = 2;

        } else {
            sceneInfo.playListIndex++;
            if (sceneInfo.playListIndex >= sceneInfo.playlist.size()) {
                sceneInfo.playListIndex = 0;
            }
        }

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                Util.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        next = true;
                        listener.onRequestBluetoothStop();
                        if (mState.equals(SceneState.PlaySmoothly)) {
                            mState = SceneState.Stop;
                            playMediaSmoothly(getCurrentTrack(), DELAY_BETWEEN_TRACKS);
                        } else {
                            mState = SceneState.Stop;
                            play(getCurrentTrack(), DELAY_BETWEEN_TRACKS, true);
                        }
                    }
                });
            }
        }, delay);
    }

    private MediaInfo getCurrentTrack() {
        return sceneInfo.playlist.get(sceneInfo.playListIndex);
    }

    private static void getWakeUpTrackLocal(final ResponseListener<Integer> listener) {
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {

                SimpleDateFormat sdf = new SimpleDateFormat("h:m a");
                String text = "Good morning John! The time is " + sdf.format(new Date());
                try {
                    String url = "http://www.translate.google.com/translate_tts?tl=en&q=" + URLEncoder.encode(text, "UTF-8");
                    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                    BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream fos = DiscoveryManager.getInstance().getContext().openFileOutput("morning.mp3", Context.MODE_PRIVATE);
                    byte[] buf = new byte[1024];
                    int size;
                    while ((size = input.read(buf)) > 0) {
                        fos.write(buf, 0, size);
                    }
                    fos.flush();
                    fos.close();
                    input.close();

                    if (server != null) {
                        server.stop();
                    }
                    server = new MediaServer(DiscoveryManager.getInstance().getContext(), "morning.mp3", 0);
                    server.start();
                    Util.runOnUI(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null)
                                listener.onSuccess(server.getListeningPort());
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }


    private void wakeUpTimer() {
        if (mState == SceneState.WakeUp) {
            return;
        }
        mState = SceneState.WakeUp;
        if (wakeUpTimer != null) {
            return;
        }

        wakeUpTimer = new Timer();
        wakeUpTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Util.runOnUI(new Runnable() {
                    @Override
                    public void run() {
                        sceneInfo.position = 0;
                        getWakeUpTrackLocal(new ResponseListener<Integer>() {
                            @Override
                            public void onSuccess(Integer object) {
                                try {
                                    String url = "http:/" + Util.getIpAddress(DiscoveryManager.getInstance().getContext()) + ":" + object + "/media.mp3";
                                    List<ImageInfo> images = Arrays.asList(new ImageInfo("http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/image1.jpg"));
                                    MediaInfo mediaInfo = new MediaInfo(url, "audio/mpeg", "Wake up message", "Wake up message", images);

                                    mState = SceneState.Stop;
                                    play(mediaInfo, 5000, false);
                                    mState = SceneState.PlaySmoothly;

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                wakeUpTimer = null;
                            }

                            @Override
                            public void onError(ServiceCommandError error) {
                            }
                        });

                    }
                });
            }
        }, WAKE_UP_DELAY);
    }

}
