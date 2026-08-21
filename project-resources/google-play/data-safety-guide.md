# Google Play Console: Data Safety Form Answers

Use this reference guide when completing the **Data Safety** questionnaire in Google Play Console for **CodeMateX**.

---

## 1. Overview & Core Answers

| Question | Answer | Rationale |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **No** | All user chats, queries, prompts, and code snippets are processed entirely on-device via LiteRT-LM and stored in local Room database. |
| **Is all user data collected by your app encrypted in transit?** | **N/A** (No user data collected or transmitted) | N/A |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | Users can delete individual sessions or clear all app data directly in the app settings / session history. |

---

## 2. Specific Data Type Declarations

| Category | Data Type | Collected? | Shared? | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Location** | Approximate / Precise location | **No** | **No** | N/A |
| **Personal Info** | Name, Email, User IDs, Phone | **No** | **No** | N/A |
| **Messages** | Emails, SMS, In-app messages | **No** | **No** | Local only |
| **Photos and Videos** | Photos, Videos | **No** | **No** | N/A |
| **Audio Files** | Voice recordings | **No** | **No** | N/A |
| **Files and Docs** | Files, Documents | **No** | **No** | N/A |
| **App Activity** | App interactions, In-app search | **No** | **No** | N/A |
| **App Info & Performance**| Crash logs, Diagnostics | **No** | **No** | Local logs only |
| **Device or Other IDs** | Device ID, Advertising ID | **No** | **No** | N/A |

---

## 3. Network Usage Clarification

CodeMateX uses the `INTERNET` and `ACCESS_NETWORK_STATE` permissions solely for:
1. Downloading on-device open-source model weights (e.g. Gemma 2B) when the user initiates a model download in the Model Manager.
2. Checking network connectivity status to prevent partial downloads on metered connections.

*No user telemetry, analytics, advertising IDs, or prompt queries are ever transmitted over the network.*
