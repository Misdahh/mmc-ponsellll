# MMC PONSEL v1.20

## Visual
- Added `mmc_logo_4k.png` at 3840x3840 for crisp branding on high-resolution screens.
- The Android UI remains density-independent; vector/scalable layouts are used where appropriate.

## Online-only mode
- Added `ACCESS_NETWORK_STATE` permission.
- App shows a blocking online-required screen when there is no active internet connection.
- Retry is available after connectivity returns.
- The app does not intentionally expose marketplace/chat/profile features while offline.

## Build
- versionCode: 10
- versionName: 1.20.0
