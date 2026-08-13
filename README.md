# US Presidential Speeches

Modern Android app for browsing US presidential speeches.

- Package: `com.uspresident.speeches`
- Ads: Google AdMob only (no Cauly)

## Features

- English UI
- List of 43 US presidents
- Speeches sorted by date for each president
- Full speech text viewer
- AdMob banner on list screens
- AdMob interstitial when opening a speech

## AdMob setup

1. Create a new app in [AdMob](https://admob.google.com) with package `com.uspresident.speeches`.
2. Create banner and interstitial ad units.
3. Replace the placeholder values in `app/src/main/res/values/strings.xml`:
   - `admob_app_id`
   - `admob_banner_unit_id`
   - `admob_interstitial_unit_id`

The current values are Google's official test IDs for development.

## Regenerate speech data

```bash
python tools/extract_speech_data.py
```

## Build

Open `PresidentialSpeeches` in Android Studio and run on a device or emulator.
