# US Presidential Speeches

Modern Android app for browsing US presidential speeches.

- Package: `com.uspresident.speeches`
- Ads: Cauly (banner + interstitial)

## Features

- English UI
- List of 43 US presidents
- Speeches sorted by date for each president
- Full speech text viewer
- Cauly banner on list screens
- Cauly interstitial when opening a speech
- Cauly interstitial for translation/TTS quota bonus

## Cauly setup

1. Media issuance ID (`cauly_app_code`) is set in `app/src/main/res/values/strings.xml` to `joVR3oib`.
2. Maven repo: `https://cauly.github.io/cauly-sdk-android-maven/maven-repo`
3. Upload the Cauly `app-ads.txt` from the Cauly dashboard to your developer domain if required.

Interstitials are request-on-show (no preload). On failure or timeout the app continues to the intended screen/action.

## Regenerate speech data

```bash
python tools/extract_speech_data.py
```

## Build

Open `PresidentialSpeeches` in Android Studio and run on a device or emulator.
