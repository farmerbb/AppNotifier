# AppNotifier

AppNotifier restores Google Play's [missing app update & app install notifications](https://www.androidpolice.com/2020/01/14/play-store-notifications-no-longer-showing-up-for-updated-apps/).

Unhappy with Google's removal of the Play Store's app update notifications? Wish you had them back? Don't worry, AppNotifier's got you covered.

## Features
* Show a notification each time an app is newly installed, or updated, on your device
* Choose whether notifications are shown for apps from Google Play, or sideloaded apps

## Limitations
* The app does not detect when apps are in the middle of installing, so there will be a short delay between when an app downloads and when the install or update notifications appear.
* Notifications are generated using data from the apps on your device, not the Play Store itself. So if an app's name differs between the Play Store listing and the actual app on your device, the latter will be used.

## Download
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Google Play"
      height="80"
      align="middle">](https://play.google.com/store/apps/details?id=com.farmerbb.appnotifier)

## How to Build
Prerequisites:
* Windows / MacOS / Linux
* JDK 8
* Android SDK
* Internet connection (to download dependencies)

Once all the prerequisites are met, make sure that the `ANDROID_HOME` environment variable is set to your Android SDK directory, then run `./gradlew assembleDebug` at the base directory of the project to start the build. After the build completes, navigate to `app/build/outputs/apk/debug` where you will end up with an APK file ready to install on your Android device.

## Contributors

* Jules Guillou (French translation)
* Benjamin Schütte (German translation)
* Sopor (Swedish translation)
