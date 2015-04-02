/*
 * SetupMultyChoiceFragment
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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.connectsdk.core.Util;
import com.connectsdk.smarthomesampler.R;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public abstract class SetupMultyChoiceFragment<T> extends Fragment {

    @InjectView(R.id.listView)
    ListView listView;

    @InjectView(R.id.textViewTitle)
    TextView textViewTitle;

    ArrayList<String> ids;

    ArrayAdapter<T> adapter;

    private View view;

    abstract void save();

    abstract String getDeviceId(T device);

    abstract String getDeviceName(T device);

    abstract String getTitleMessage();

    abstract void startDeviceDiscovery();

    abstract void stopDeviceDiscovery();

    public static Fragment setArguments(Fragment fragment, ArrayList<String> ids) {
        Bundle args = new Bundle();
        args.putSerializable("ids", ids);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            this.ids = (ArrayList<String>) args.getSerializable("ids");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_single_choice, container, false);
        ButterKnife.inject(this, view);
        adapter = new DeviceAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setEmptyView(view.findViewById(R.id.empty));
        textViewTitle.setText(getTitleMessage());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        startDeviceDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopDeviceDiscovery();
    }

    @OnClick(R.id.btnNext)
    public void onNextStep() {
        SparseBooleanArray checked = listView.getCheckedItemPositions();
        ids.clear();
        if (adapter != null) {
            for (int i = 0; i < checked.size(); i++) {
                if (checked.get(checked.keyAt(i))) {
                    T device = adapter.getItem(checked.keyAt(i));
                    ids.add(getDeviceId(device));
                }
            }
        }
        save();
    }

    void updateData(final List<T> devices) {
        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                adapter.clear();
                adapter.addAll(devices);
                adapter.notifyDataSetChanged();
                for (T device : devices) {
                    if (ids.contains(getDeviceId(device))) {
                        listView.setItemChecked(adapter.getPosition(device), true);
                    }
                }
                Log.d("", "WINK update adapter");
            }
        });
    }

    void updateData(final T device) {
        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                if (adapter.getPosition(device) < 0) {
                    adapter.add(device);
                    adapter.notifyDataSetChanged();
                    view.requestLayout();
                    view.invalidate();

                    if (ids.contains(getDeviceId(device))) {
                        listView.setItemChecked(adapter.getPosition(device), true);
                    }
                }
            }
        });
    }

    private class DeviceAdapter extends ArrayAdapter<T> {

        public DeviceAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_multiple_choice);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            T device = getItem(position);
            convertView = super.getView(position, convertView, parent);

            TextView title = (TextView)convertView.findViewById(android.R.id.text1);
            title.setText(getDeviceName(device));

            return convertView;
        }
    }

}
