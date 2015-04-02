/*
 * SetupSingleChoiceFragment
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.connectsdk.core.Util;
import com.connectsdk.smarthomesampler.R;

import java.util.Collection;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public abstract class SetupSingleChoiceFragment<T> extends Fragment {

    @InjectView(R.id.listView)
    ListView listView;

    @InjectView(R.id.textViewTitle)
    TextView textViewTitle;

    ArrayAdapter<T> adapter;

    private View view;

    abstract void save(T device);

    abstract String getDeviceName(T device);

    abstract String getTitleMessage();

    abstract int getPosition();

    abstract void startDeviceDiscovery();

    abstract void stopDeviceDiscovery();


        @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_multy_choice, container, false);
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
        int checkedPosition = listView.getCheckedItemPosition();
        if (checkedPosition >= 0) {
            T device = adapter.getItem(checkedPosition);
            save(device);
        } else {
            save(null);
        }
    }

    void updateData(final Collection<T> devices) {
        Util.runOnUI(new Runnable() {
            @Override
            public void run() {
                adapter.clear();
                adapter.addAll(devices);
                adapter.notifyDataSetChanged();
                view.requestLayout();
                view.invalidate();
                listView.setItemChecked(getPosition(), true);
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
                    listView.setItemChecked(getPosition(), true);
                }
            }
        });
    }

    private class DeviceAdapter extends ArrayAdapter<T> {

        public DeviceAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_single_choice);
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
