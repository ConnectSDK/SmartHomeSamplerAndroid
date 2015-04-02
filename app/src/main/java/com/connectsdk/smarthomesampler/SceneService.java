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

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import com.connectsdk.smarthomesampler.scene.SceneController;


public class SceneService extends Service {

    private static final int NOTIFICATION_ID = 1;

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
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Wake Lock");
        wakeLock.acquire();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.smarthome_service_is_working))
                .setContentIntent(pendingIntent).build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        wakeLock.release();
        if (controller != null) {
            controller.disconnect();
        }
    }
}
