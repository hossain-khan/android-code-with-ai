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

| Form Field | Value / Declaration Selection |
| :--- | :--- |
| **Foreground Service Type** | `DATA_SYNC` (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`) |
| **Use Case Category** | **Data transfer / File download initiated by user** *(Ensure "Network Backup" or background sync is NOT selected)* |
| **Is the service user-initiated?** | **Yes** — Explicitly triggered when the user taps "Download" on an on-device AI model in the AI Models picker. |
| **Is an ongoing notification shown?** | **Yes** — Displays a persistent, user-perceptible notification in the notification shade showing live download progress (MB downloaded / total MB), percentage, and a "Cancel" action button. |
| **Why can't WorkManager standard background jobs be used?** | *See Justification Statement below* |

### Policy Justification Statement (Copy & Paste to Play Console):
> *"CodeMateX downloads large on-device LLM model weights (2.6GB–3.7GB) upon explicit user request. A foreground service with a persistent, perceptible progress notification is required to prevent the OS from terminating multi-minute downloads when the app is backgrounded or in Doze mode, while providing the user with real-time download progress and cancellation controls in the notification shade."*

---

## 2. Notification Permission (`POST_NOTIFICATIONS`)

### Manifest Declaration:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Purpose & User Experience:
- **Android 13+ (API 33+)**: Required to display the active foreground download notification in the system notification shade.
- **In-App Rationale Dialog**: When the user initiates a model download in the AI Models screen, CodeMateX presents an in-context educational Material 3 dialog explaining that notifications are used to show real-time download progress for multi-gigabyte models and alert the user when their on-device AI tutor is ready for offline use.

---

## 3. Demonstration Video Requirements for Google Play Submission

Google Play review requires a public video link (YouTube unlisted / Google Drive public link) demonstrating that the Foreground Service is **user-initiated and perceptible**:

1. **Step 1**: Open the app and navigate to **AI Models** (or tap "Manage Models").
2. **Step 2**: Tap **Download** on an available model (e.g. Gemma 2B).
3. **Step 3**: Show the in-context notification permission rationale dialog and tap **Enable Notifications**.
4. **Step 4**: Swipe down the **system notification shade** to clearly show the active foreground service notification displaying:
   - "Downloading [Model Name]"
   - Live progress bar + percentage / MB downloaded
   - "Cancel" button
5. **Step 5**: Tap **Cancel** or allow the download to progress to show user control.

---

## 4. Official Reference Links:
- [Android 14 Foreground Service Types Policy (Google Play)](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Android 14 Data Sync Foreground Service Guidance](https://developer.android.com/about/versions/14/changes/fgs-types-special-use#data-sync)
- [Android Notification Runtime Permissions](https://developer.android.com/develop/ui/views/notifications/notification-permission)

