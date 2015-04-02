/*
 * IScene
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

import com.belkin.wemo.localsdk.WeMoSDKContext;
import com.connectsdk.smarthomesampler.adapter.HueAdapter;
import com.connectsdk.smarthomesampler.adapter.WinkAdapter;

public interface IScene {

    static enum State {
        Play,
        Stop,
        WakeUp, Fullstop, Smoothstop,
    }

    public void setHueAdapter(HueAdapter mHueAdapter);

    public void setWeMoContext(WeMoSDKContext weMoContext);
    
    public void setWinkAdapter(WinkAdapter winkAdapter);

    public void setSceneInfo(SceneInfo sceneInfo);

    public SceneInfo getSceneInfo();
    
    public void setState(IScene.State state);

    public State getState();

    public boolean isConnected();

    public SceneConfig getConfig();

    public String getName();
}
