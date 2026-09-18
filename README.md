# US Presidential Speeches

Modern Android app for browsing US presidential speeches.

- Package: `com.uspresident.speeches`
- Ads: Cauly banner waterfall (Cauly → Pangle → Unity) + interstitial waterfall (Cauly → Pangle → Unity)

## Features

- English UI
- List of 43 US presidents
- Speeches sorted by date for each president
- Full speech text viewer
- Banner waterfall on list screens (Cauly → Pangle → Unity)
- Interstitial waterfall after opening a speech (navigate first, then ad)
- Interstitial waterfall for translation/TTS quota bonus

## Ad setup

IDs are in `app/build.gradle.kts` `BuildConfig` fields (`CAULY_*`, `PANGLE_*`, `UNITY_*`).

Interstitials are request-on-show for Cauly, then Pangle, then Unity on failure. UI is never blocked waiting for an ad.

## Regenerate speech data

```bash
python tools/extract_speech_data.py
```

## Build

Open `PresidentialSpeeches` in Android Studio and run on a device or emulator.
