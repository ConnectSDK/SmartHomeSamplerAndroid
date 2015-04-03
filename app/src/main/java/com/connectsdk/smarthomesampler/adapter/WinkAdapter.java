/*
 * WinkAdapter
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
package com.connectsdk.smarthomesampler.adapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WinkAdapter {

    private static final String DEVICE_TYPE_BULB = "light_bulbs";
    private WinkClient wink;
    
    public void connect(final JSONObject credentials) {
        wink = new WinkClient();
        try {
            wink.oauth(credentials);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    
    public void updateBulb(String id, boolean enabled, double brightness) throws JSONException, IOException {
        JSONObject state = new JSONObject();
        state.put("powered", enabled);
        state.put("brightness", brightness);
        
        JSONObject body = new JSONObject();
        body.put("desired_state", state);
        
        wink.updateDevice(DEVICE_TYPE_BULB, id, body);        
    }

    public List<JSONObject> getGeBulbIds() throws IOException, JSONException {
        JSONObject devices = wink.loadAllDevices();
        JSONArray jsonArray = devices.getJSONArray("data");
        List<JSONObject> geBulbs = new ArrayList<JSONObject>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonItem = jsonArray.getJSONObject(i);
            if (jsonItem.has("light_bulb_id") && jsonItem.has("model_name") && "GE light bulb".equals(jsonItem.get("model_name"))) {
                geBulbs.add(jsonItem);
            }
        }
        return geBulbs;
    }

    static class WinkClient {
        private static final String HOST = "https://winkapi.quirky.com";
        private static final String POST = "POST";
        private static final String GET = "GET";
        private static final String PUT = "PUT";

        private String accessToken;
        private String tokenType;

        public void oauth(JSONObject credentials) throws IOException, JSONException {
            JSONObject response = winkRequest(POST, "/oauth2/token", credentials);
            this.accessToken = response.getString("access_token");
            this.tokenType = response.getString("token_type");
        }

        public JSONObject loadAllDevices() throws IOException, JSONException {
            return winkRequest(GET, "/users/me/wink_devices", null);
        }
        
        public JSONObject loadDevice(String type, String id) throws IOException, JSONException {
            return winkRequest(GET, "/" + type + "/" + id, null);
        }

        public JSONObject updateDevice(String type, String id, JSONObject body) throws IOException, JSONException {
            return winkRequest(PUT, "/" + type + "/" + id, body);
        }

        private JSONObject winkRequest(String method, String path, JSONObject body) throws IOException, JSONException {
            HttpURLConnection connection = (HttpURLConnection) new URL(HOST + path).openConnection();
            connection.setRequestMethod(method);

            if (accessToken != null && tokenType != null) {
                connection.setRequestProperty("Authorization", tokenType + " " + accessToken);
            }

            if (method.equals(POST) || method.equals(PUT)) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                // write
                PrintWriter writer = new PrintWriter(connection.getOutputStream(), true);
                writer.write(body.toString());
                writer.flush();
                writer.close();
            }

            // read
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while (null != (line = reader.readLine())) {
                sb.append(line);
            }
            reader.close();
            connection.disconnect();

            // parse json
            return new JSONObject(sb.toString());
        }
    }
}
