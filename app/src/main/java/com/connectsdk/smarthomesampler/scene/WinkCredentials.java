/*
 * WinkCredentials
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

import org.json.JSONException;
import org.json.JSONObject;

public class WinkCredentials {

    public final static String CLIENT_ID        = "CHANGE_ME";
    public final static String CLIENT_SECRET    = "CHANGE_ME";
    public final static String USERNAME         = "CHANGE_ME";
    public final static String PASSWORD         = "CHANGE_ME";

    public JSONObject getCredentials() throws JSONException {
        return new JSONObject("{\n" +
                "            \"client_id\": \"" + CLIENT_ID + "\",\n" +
                "            \"client_secret\": \"" + CLIENT_SECRET + "\",\n" +
                "            \"username\": \"" + USERNAME + "\",\n" +
                "            \"password\": \"" + PASSWORD + "\",\n" +
                "            \"grant_type\": \"password\"\n" +
                "        }");
    }

}