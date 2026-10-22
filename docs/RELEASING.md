# Cutting a release

Checklist for turning a tested build into a public GitHub release. Don't skip steps — an APK attached to a release is a promise that it was built and verified exactly as described.

## 1. Version

Bump `versionCode` (+1) and `versionName` in `app/build.gradle`.

## 2. Build and test

From the project root:

```bat
gradlew.bat testDebugUnitTest assembleRelease
```

All unit tests must pass (see the XML reports under `app\build\test-results\testDebugUnitTest\`). The signed APK lands at `app\build\outputs\apk\release\app-release.apk`.

## 3. Publish the file and verify it

1. Copy the APK to the download location and take its SHA-256.
2. Fetch the first kilobyte back over HTTPS with a `Range` request and compare hashes. Only exact match counts as verified.

Until that check passes, the previous download link stays the current one — never point users at an unverified file.

## 4. Create the GitHub release

Tag and title use the version (`v1.11.1` style):

```bat
gh release create vX.Y.Z path\to\app.apk --title "vX.Y.Z" --notes "..."
```

Notes contain, at minimum: what changed since the last release, the SHA-256 of the attached APK, the minimum Android version, and the TV requirement (Samsung Tizen, Developer Mode, same Wi-Fi). Keep claims inside what was actually tested — see [COMPATIBILITY.md](../COMPATIBILITY.md).

## 5. After the release

Update the download link wherever it's published only after steps 2–4 are green. Old releases stay up until the new one is confirmed working; then remove the superseded one so there is exactly one current APK.
