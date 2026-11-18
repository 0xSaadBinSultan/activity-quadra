# Activity Trio

**GitHub, Codeforces and LeetCode activity on your Android home screen.**

A lightweight native Java app with a combined three-graph widget and three individual
widgets. Resizable, offline-capable and ad-free. Android 8.0+; target API 35.
No WebView, account tokens, embedded personal profiles or analytics.

[Download the APK](https://github.com/0xSaadBinSultan/activity-trio/releases/latest)
 · [Report an issue](https://github.com/0xSaadBinSultan/activity-trio/issues)
 · [MIT license](LICENSE)

<img src="docs/combined-widget.png" alt="Synthetic preview of the three activity graphs" width="380">

The preview uses synthetic test data. A fresh install starts with empty usernames.

## Install and configure

1. Install `ActivityTrio-v1.3.0.apk` and open Activity Trio.
2. Enter your public GitHub, Codeforces and/or LeetCode usernames in Profiles.
   Enter at least one; leave unused services blank. No login or API key is needed.
3. The timezone defaults to your device timezone. Year `0` follows the current year.
4. Choose **Add widget → All three together**, or pick an individual service.
   If pinning is unavailable, long-press Home → Widgets → Activity Trio.
5. Long-press to resize. Tap a graph to open its interactive date details.

This is the same app as Activity Quadra, with its original Activity Trio name restored.
The package ID and signing identity are unchanged, so the official APK installs over
the previous app and preserves the remaining three profiles and their cached data.
The discontinued fourth service, its profile field, imports, graph and widget provider
have been removed. No uninstallation is required. If a launcher keeps an older combined
layout, remove and re-add only that home-screen widget.

## Widget behavior

The combined widget stacks three full-year graphs in one panel, targeting 4 × 4
launcher cells by default. Long-press to resize it. One weighted layout works in
portrait and landscape on phones and tablets; cells stay square. The launcher
controls the available space and resize steps. Tap a graph for readable dates and
counts in the app; pinch to zoom or use Choose date.

**Add widget → Preview combined** shows a read-only preview inside the app. Opening
the app or tapping Refresh resends cached graphs to Home. Feeds publish independently
as they finish; one rejected widget update cannot block other widgets. Image payloads
are bounded across launcher size reports. Failed requests retain the last successful
cache, timestamp and a visible error message. Blank usernames trigger no requests.

Configured feeds refresh about every six hours while at least one widget is present.
Android battery restrictions can delay background jobs. Manual refresh has cooldowns
and server-limit backoff; removing every widget cancels the periodic job. The app
works offline from its private cache. The tablet dashboard uses two columns and a
full-width third graph; phone dashboard cards scroll normally.

## What the numbers mean

- **GitHub:** public contributions and GitHub's own contribution calendar dates.
  No private-repository access is requested.
- **Codeforces:** submission attempts, including unsuccessful attempts, grouped by
  the selected timezone. These are not distinct solved-problem counts. History is
  paginated with at least 2.2 seconds between page requests. Incomplete histories
  never overwrite a good cache with partial totals.
- **LeetCode:** all-time unique solved / total available problems, with Easy, Medium
  and Hard counts in the app. The heatmap and submission total cover the selected
  year. Calendar dates stay in UTC as encoded by LeetCode. Its public GraphQL profile
  endpoint is read without login or cookies; it is not a guaranteed stable external API.

## Privacy

No GitHub/Google tokens, OAuth clients, signing secrets or personal activity caches
are included in the source or APK. Only configured public usernames are sent over
HTTPS to GitHub, Codeforces or LeetCode, exposing the device IP to those services as
normal. Profile links use your browser. Cached activity and preferences remain in
app-private storage, with Android app backup disabled. No Google Calendar, Tasks,
Drive or notes access is used.

## Build and validate

Use JDK 21, Gradle 8.9, Android Gradle Plugin 8.7.3, Platform 35 and Build Tools 35.0.0.
The Gradle wrapper pins the official distribution SHA-256. Install Google's SDK
packages after reviewing its SDK license, then set the local SDK path:

```sh
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
JAVA_HOME=/path/to/jdk-21 ./gradlew --no-daemon testDebugUnitTest lintRelease assembleRelease
```

Tests cover public feed parsing, empty profiles, optional services, cache isolation,
upgrade preservation of the remaining three profiles, removed-provider registration,
UTC/timezone boundaries, graph hit testing, actual image changes through RemoteViews
parcel/reapply, bitmap budgets, per-widget failure isolation, feed-by-feed publication,
and phone/tablet rendering on Android APIs 28 and 35. Synthetic screenshots go to
`app/build/reports/screenshots/`. Optional live-parser fixtures are local files outside
the source tree. Host-side tests do not replace checking the installed APK on a
physical device and its launcher.

To create a signed release:

```sh
JAVA_HOME=/path/to/jdk-21 ANDROID_HOME=/path/to/android-sdk ./build-release.sh
```

The helper keeps the private signing identity outside the repo at
`~/.local/share/activity-trio/signing` (or `ACTIVITY_TRIO_SIGNING_DIR`). Passwords are
read from private files. Back this directory up securely outside Git to sign compatible
updates. A differently signed fork requires uninstalling the official app first,
which removes its local settings. Build output, SDK paths, credentials and signing
files are excluded from Git.

Official references: [Android widgets](https://developer.android.com/develop/ui/views/appwidgets/overview),
[SDK Manager](https://developer.android.com/tools/sdkmanager),
[Codeforces API](https://codeforces.com/apiHelp/methods).

## License

Original app code: **MIT**, copyright 2026 0xSaadBinSultan. See [LICENSE](LICENSE),
[NOTICE](NOTICE) and [CONTRIBUTING.md](CONTRIBUTING.md). Upstream components retain
their own licenses.
