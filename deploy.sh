#!/bin/bash

cd web && npm run build && npm run install
cd ..

adb disconnect 192.168.0.100
adb connect 192.168.0.100
#./gradlew assambleDebug
./gradlew installDebug
#adb shell am start -n "com.github.kidplayer/com.github.kidplayer.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -D
adb shell am start -n "com.github.kidplayer/com.github.kidplayer.MainActivity"


adb disconnect 192.168.0.100
