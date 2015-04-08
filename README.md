#Smart Home Sampler App for Android

## About

This demo app demonstrates a scenario of using various Smart Home devices in two home scenes. They represent a living room and a family room, each containing a media device, light bulbs, and possibly other devices. The supported devices come from different categories (media players, light bulbs, switches, and iBeacons) and multiple manufacturers.

We believe the Smart Homes of the future, are not going to be driven by devices from a single manufacturer, instead a network of devices from various manufacturers.

The scenario of the app is:

1. You enter the living room, which is detected by an iBeacon, 
2. A playlist starts to play on a TV or speaker, and the light bulbs change color to match one of the colors of the album art during playback.
3. Then the user moves from the living room scene to the family room scene.
4. Where the session information is transfered from the living room to the family room.
    - The devices in the living switch off and the session is picked up in the family room
5. The user put the scene to sleep using voice command (to replicate control using Siri or Google Now or other voice engine/assitants)
    - The speaker fades out  the music, while the LED bulb fade out and switch off.
6. The Scene wakes up after a defined time - to mimic waking up from an alarm.
    - The LEd Bulbs switch on along with speaker.

For additional information on Connect SDK, visit [http://connectsdk.com/discover/](http://connectsdk.com/discover/).

## Prerequisites

Required devices:

* LG WebOS TV or DLNA-compatible media device for each scene (two total), such as Sonos.

The app has been tested and works with these devices:

* [LG WebOS 2014 TV](http://www.lg.com/us/experience-tvs/smart-tv)
* [Sonos PLAY:1 speaker](http://www.sonos.com/sonos-shop/products/play1)
* [Philips Hue hub and bulbs](http://www2.meethue.com/en-us/)
* [Belkin WeMo Switch](http://www.belkin.com/us/p/F7C027fc/)
* [Wink hub](http://www.wink.com/products/wink-hub/) + [GE link light bulb](http://gelinkbulbs.com)
* [StickNFind iBeacons](https://www.sticknfind.com/sticknfind.aspx)

**Important**: Make sure all the WiFi-supported devices (WebOS TV, Sonos speaker, Philips Hue hub, WeMo switch, and Wink hub) and your Android device with the app are connected to the same WiFi network. To configure the devices, you need to use their respective apps.

##Dependencies
- Android SDK v21
- [Android v4 appcompat library](http://developer.android.com/tools/support-library/features.html#v4-appcompat)
- [Android v7 palette library](http://developer.android.com/tools/support-library/features.html#v7-palette)
- [Connect-SDK-Android](https://github.com/ConnectSDK/Connect-SDK-Android)
- [NanoHttpd](https://github.com/NanoHttpd/nanohttpd)
- [Butter Knife](http://jakewharton.github.io/butterknife/)
- [Picasso](http://square.github.io/picasso/)
- [Philips Hue Java SDK](http://www.developers.meethue.com/documentation/java-multi-platform-and-android-sdk)
- [Belkin WeMo Local SDK for Android](http://developers.belkin.com/wemo/sdk)

##Setup with Android Studio
1. Download the SmartHomeSamplerAndroid project
    ```
    git clone https://github.com/ConnectSDK/SmartHomeSamplerAndroid.git
    ```
    
2. Download [Philips Hue Java SDK](http://www.developers.meethue.com/documentation/java-multi-platform-and-android-sdk) (huelocalsdk.jar and huesdkresources.jar) and put it into the app/libs folder.
3. Download [Belkin WeMo Local SDK for Android](http://developers.belkin.com/wemo/sdk) and put it into the app/libs folder.
4. Import the SmartHomeSamplerAndroid project into Android Studio
5. Modify app/src/main/java/com/connectsdk/smarthomesampler/scene/WinkCredentials.java and set your Wink credentials in that file.
6. Execute this command to avoid adding your credentials to git
    ```
    git update-index --assume-unchanged app/src/main/java/com/connectsdk/smarthomesampler/scene/WinkCredentials.java
    ```

##License

Copyright (c) 2015 LG Electronics.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

> http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
