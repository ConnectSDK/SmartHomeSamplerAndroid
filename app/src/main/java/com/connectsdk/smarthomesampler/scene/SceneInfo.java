/*
 * SceneInfo
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

import com.connectsdk.core.MediaInfo;

import java.util.List;

public class SceneInfo {
    int position;
    int playListIndex;
    final List<MediaInfo> playlist;
    
    public SceneInfo(List<MediaInfo> playlist) {
        this.playlist = playlist;
    }

    public int getPosition() {
        return position;
    }

    public int getPlayListIndex() {
        return playListIndex;
    }

    public List<MediaInfo> getPlaylist() {
        return playlist;
    }
}
