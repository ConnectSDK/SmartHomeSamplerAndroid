/*
 * SceneService
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
package com.connectsdk.smarthomesampler;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.connectsdk.smarthomesampler.scene.SceneController;


public class SceneService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private SceneController controller;
    private PowerManager.WakeLock wakeLock;

    public void createController(IUserNotificationListener listener) {
        if (controller != null) {
            controller.disconnect();
            controller = null;
        }
        controller = new SceneController(this, listener);
    }

    public void wakeUp() {
        if ( controller != null) {
            controller.wakeUp();
        }
    }

    public void play(String id) {
        if (controller !=null) {
            controller.playScene(id);
        }
    }

    public void stop() {
        if (controller != null) {
            controller.stop();
        }
    }

    public boolean isConnected() {
        return controller != null && controller.isConnected();
    }

    public void onEnterBeacon(String address) {
        if (controller != null) {
            controller.onEnterBeacon(address);
        }
    }

    public class LocalBinder extends Binder {
        SceneService getService() {
            return SceneService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("", "sceneservice bind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d("", "sceneservice unbind");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("", "scene service created");
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Wake Lock");
        wakeLock.acquire();
        Log.d("", "sceneservice create");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
        Log.d("", "scene service destroyed");
        if (controller != null) {
            controller.disconnect();
        }
        Log.d("", "sceneservice destroy");
    }
}
