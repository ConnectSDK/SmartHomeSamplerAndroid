/*
 * MainActivity
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

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.connectsdk.smarthomesampler.dialog.AcknowledgementsFragmentDialog;
import com.connectsdk.smarthomesampler.scene.SceneController;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends ActionBarActivity implements IUserNotificationListener {

    private static final int REQUEST_RECOGNIZE_SPEECH = 2;

    @InjectView(R.id.textViewDebug)
    TextView textViewDebug;

    @InjectView(R.id.progressBar)
    ProgressBar progressBar;

    @InjectView(R.id.sceneLayout)
    LinearLayout sceneLayout;


    private final BroadcastReceiver beaconReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String address = intent.getStringExtra("beacon");
            double distance = intent.getDoubleExtra("distance", 0);
            textViewDebug.setText("closest beacon: " + address + " " + distance);

            if (mService != null) {
                pauseScanning();
                mService.onEnterBeacon(address);
            }
        }
    };

    private BluetoothAdapter bluetoothAdapter;

    private AlertDialog pairingDialog;

    private SceneService mService;

    private boolean mBound;

    private boolean bluetoothServiceStarted;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SceneService.LocalBinder binder = (SceneService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            if (mService.isConnected()) {
                sceneLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            } else {
                startSceneController();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = btManager.getAdapter();
        disableBluetooth();

        startService(new Intent(this, SceneService.class));
        bindService(new Intent(this, SceneService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (mBound) {
                unbindService(mServiceConnection);
                mBound = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_license:
                AcknowledgementsFragmentDialog.newInstance().show(getSupportFragmentManager(), "info_dialog");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void disableBluetooth() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
            }
        }
    }

    private void checkBluetoothAndStartService() {
        if (!bluetoothServiceStarted) {
            bluetoothServiceStarted = true;
            startService(new Intent(this, BluetoothService.class));
            registerReceiver(beaconReceiver, new IntentFilter(BluetoothService.ACTION_NEW_BEACON));
        }
    }

    @OnClick(R.id.buttonSay)
    public void onSayClick() {
        listenToSpeech();
    }

    @OnClick(R.id.buttonHold)
    public void onStopClick() {
        if (mService != null) {
            mService.stop();
        }
    }

    @OnClick(R.id.buttonStop)
    public void onDisconnectClick() {
        disconnectSceneController();
    }


    @OnClick(R.id.buttonConfigure)
    public void onConfigureClick() {
        Intent intent = new Intent(this, SetupActivity.class);
        intent.putExtra(SetupActivity.EXTRA_FORCE, true);
        startActivity(intent);
        disconnectSceneController();
    }

    @OnClick(R.id.buttonScene1)
    public void onScene1Click() {
        pauseScanning();
        mService.play(SceneController.SCENE_ID_FIRST);
    }

    @OnClick(R.id.buttonScene2)
    public void onScene2Click() {
        pauseScanning();
        mService.play(SceneController.SCENE_ID_SECOND);
    }


    @OnClick(R.id.buttonWakeUp)
    public void onWakeUp() {
        pauseScanning();
        mService.wakeUp();
    }

    private void pauseScanning() {
        Intent service = new Intent(MainActivity.this, BluetoothService.class);
        service.setAction(BluetoothService.ACTION_PAUSE_SCANNING);
        startService(service);
    }

    private void startSceneController() {
        disableBluetooth();
        mService.createController(this);
    }

    private void disconnectSceneController() {
        unbindService(mServiceConnection);
        stopService(new Intent(this, SceneService.class));

        try {
            unregisterReceiver(beaconReceiver);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        stopService(new Intent(this, BluetoothService.class));

        finish();
    }


    @Override
    public void onPairingRequest(final String device) {
        if (pairingDialog == null) {
            pairingDialog = new AlertDialog
                    .Builder(this)
                    .setTitle("Pairing with " + device)
                    .setMessage("Please confirm the connection on your " + device)
                    .setPositiveButton("Okay", null)
                    .create();
            pairingDialog.show();
        }
    }

    @Override
    public void onSceneConnected(String id) {
        sceneLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);

        checkBluetoothAndStartService();
    }

    @Override
    public void onRequestBluetoothStop() {
        pauseScanning();
    }

    private void listenToSpeech() {
        Intent listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        listenIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        listenIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a word!");
        listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        startActivityForResult(listenIntent, REQUEST_RECOGNIZE_SPEECH);
    }
}
