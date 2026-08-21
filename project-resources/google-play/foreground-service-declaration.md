# Google Play Console: Foreground Service & Permissions Declaration

This document contains the exact declaration details and policy justifications required by the **Google Play Console** for **CodeMateX** when declaring Foreground Service permissions (`FOREGROUND_SERVICE_DATA_SYNC`) and runtime notifications (`POST_NOTIFICATIONS`).

---

## 1. Foreground Service Type: `DATA_SYNC`

### Manifest Declarations:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

### Google Play Console Policy Declaration Form:

| Form Field | Value / Declaration Copy |
| :--- | :--- |
| **Foreground Service Type** | `DATA_SYNC` (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`) |
| **Use Case Category** | *Data transfer / File download initiated by user* |
| **Is the service user-initiated?** | **Yes** — Initiated when user taps "Download Model" in AI Models picker |
| **Is an ongoing notification shown?** | **Yes** — Displays ongoing download progress percentage and cancel button |
| **Why can't WorkManager standard background jobs be used?** | *See Justification Statement below* |

### Policy Justification Statement (Copy & Paste to Play Console):
> *"CodeMateX downloads large on-device LLM model weights (2.6GB–3.7GB) upon explicit user request. A foreground service with a persistent progress notification is required to prevent the OS from terminating multi-minute downloads when the app is backgrounded, while providing the user with real-time download progress and cancellation controls."*

---

## 2. Notification Permission (`POST_NOTIFICATIONS`)

### Manifest Declaration:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Purpose & User Experience:
- **Android 13+ (API 33+)**: Required to display the active foreground download notification in the system notification shade.
- **In-App Rationale Dialog**: On first launch, CodeMateX presents an educational Material 3 dialog explaining why notifications are requested (live progress tracking and offline model readiness alerts) before invoking the system prompt via Google's `accompanist-permissions` library.

---

## 3. Official Reference Links:
- [Android 14 Foreground Service Types Policy (Google Play)](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Android 14 Data Sync Foreground Service Guidance](https://developer.android.com/about/versions/14/changes/fgs-types-special-use#data-sync)
- [Android Notification Runtime Permissions](https://developer.android.com/develop/ui/views/notifications/notification-permission)
