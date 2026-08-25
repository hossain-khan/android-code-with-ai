# Android Signing Keystores

This directory contains keystores for signing Android builds for **CodeMateX**.

---

## 1. Debug Keystore

For local development, test executions, and CI runs without a production keystore configured, the debug keystore (`debug.keystore`) is used with standard Android debug credentials:
- **Store Password**: `android`
- **Key Alias**: `androiddebugkey`
- **Key Password**: `android`

> [!NOTE]
> Local release builds (`./gradlew assembleRelease`) automatically fall back to `debug.keystore` when environment variables (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`) are not set. See [app/build.gradle.kts](../app/build.gradle.kts).

Reference: [Android Developers: App Signing in Debug Mode](https://developer.android.com/studio/publish/app-signing#debug-mode)

---

## 2. Release Keystore (Production)

For official production releases, CodeMateX uses a dedicated production keystore configured via GitHub Actions repository secrets:
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

### Production Certificate Fingerprint (SHA-256)
```text
c0547bb27a85df762bf6a96e2f1837c76891eb294efb70f05f778fef1db441e8
```

For full details on configuring secrets, release verification, and cutting new versions, see [RELEASE.md](../RELEASE.md).

---

## 3. Related Workflows

Automated CI/CD workflows utilizing signing:
1. **[`android-release.yml`](../.github/workflows/android-release.yml)**: Builds signed production APK & AAB, cryptographically validates the SHA-256 certificate fingerprint with `apksigner`, and attaches the verified binaries to the GitHub Release.
2. **[`test-keystore-apk-signing.yml`](../.github/workflows/test-keystore-apk-signing.yml)**: Validates keystore decoding, signing configuration, and `apksigner` certificate fingerprint matching.
