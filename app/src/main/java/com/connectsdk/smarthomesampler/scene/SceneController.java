/*
 * SceneController
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

import com.belkin.wemo.localsdk.WeMoSDKContext;
import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.Util;
import com.connectsdk.smarthomesampler.IUserNotificationListener;
import com.connectsdk.smarthomesampler.adapter.HueAdapter;
import com.connectsdk.smarthomesampler.adapter.WinkAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SceneController {

    public static final String SCENE_ID_FIRST = "scene#1";

    public static final String SCENE_ID_SECOND = "scene#2";

    private final WeMoSDKContext mWeMoSDKContext;

    private final WinkAdapter mWinkAdapter;

    private final HueAdapter mHueAdapter;

    IScene currentScene;

    final Map<String, IScene> scenes = new HashMap<String, IScene>();

    private final IUserNotificationListener userNotificationListener;


    public SceneController(Context context, IUserNotificationListener userNotificationListener) {
        this.userNotificationListener = userNotificationListener;
        mWeMoSDKContext = new WeMoSDKContext(context.getApplicationContext());
        mWinkAdapter = new WinkAdapter();
        mHueAdapter = new HueAdapter(context);
        connectWink();
        createScene(context, SCENE_ID_FIRST);
        createScene(context, SCENE_ID_SECOND);
        mWeMoSDKContext.refreshListOfWeMoDevicesOnLAN();
        mHueAdapter.startDiscovery();
    }

    private void connectWink() {
        Util.runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    mWinkAdapter.connect(new WinkCredentials().getCredentials());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    List<MediaInfo> getPlaylist() {
        MediaInfo media;
        List<MediaInfo> playlist = new ArrayList<MediaInfo>();
        List<ImageInfo> images;

        images = Arrays.asList(new ImageInfo("http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/sample.png"));
        media = new MediaInfo("http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/hydrate.mp3", "audio/mp3", "title", "description", images);
        playlist.add(media);

        images = Arrays.asList(new ImageInfo("http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/sample2.png"));
        media = new MediaInfo("http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/fantasy_in_c_major.mp3", "audio/mp3", "title2", "description2", images);
        playlist.add(media);

        images = Arrays.asList(new ImageInfo("http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/sample3.png"));
        media = new MediaInfo("http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/symphony.mp3", "audio/mp3", "title3", "description3", images);
        playlist.add(media);

        return playlist;
    }

    private void createScene(Context context, String name) {
        SceneConfig config = SceneConfig.loadFromPreferences(context, name);
        IScene scene = new Scene(name, config, userNotificationListener);
        SceneInfo info = new SceneInfo(getPlaylist());
        scene.setSceneInfo(info);
        scene.setWeMoContext(mWeMoSDKContext);
        scene.setWinkAdapter(mWinkAdapter);
        scene.setHueAdapter(mHueAdapter);
        scenes.put(name, scene);
    }

    public void stop() {
        for (IScene scene : scenes.values()) {
            scene.setState(IScene.State.Stop);
        }
    }

    public void disconnect() {
        for (IScene scene : scenes.values()) {
            scene.setState(IScene.State.Fullstop);
        }
    }

    public synchronized void playScene(String key) {
        IScene prevScene = currentScene;
        currentScene = scenes.get(key);
        if (prevScene != null && prevScene != currentScene) {
            currentScene.setSceneInfo(prevScene.getSceneInfo());
            prevScene.setState(IScene.State.Stop);
        }
        currentScene.setState(IScene.State.Play);
    }

    public void onEnterBeacon(final String closestBeacon) {
        IScene nextScene = null;
        for (IScene scene : scenes.values()) {
            if (scene.getState().equals(IScene.State.WakeUp)) {
                return;
            }
            if (closestBeacon.equals(scene.getConfig().beacon)) {
                nextScene = scene;
            }
        }

        if (nextScene != null) {
            playScene(nextScene.getName());
        }
    }


    public void wakeUp() {
        if (currentScene != null) {
            currentScene.setState(IScene.State.Smoothstop);
        } else {
            currentScene = scenes.get(SCENE_ID_FIRST);
            currentScene.setState(IScene.State.WakeUp);
        }
    }


    public boolean isConnected() {
        for (IScene scene : scenes.values()) {
            if (!scene.isConnected()) {
                return false;
            }
        }
        return true;
    }
}
