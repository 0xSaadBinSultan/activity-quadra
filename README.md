# Activity Quadra

**GitHub, Codeforces, TryHackMe and LeetCode activity on your Android home screen.**

A lightweight, ad-free native Java app with a combined **4-in-1** widget and four
individual widgets. Android 8.0+; target API 35. No WebView, account tokens or analytics.

[Download the APK](https://github.com/0xSaadBinSultan/activity-quadra/releases/latest)
 · [Report an issue](https://github.com/0xSaadBinSultan/activity-quadra/issues)
 · [MIT license](LICENSE)

<img src="docs/combined-widget.png" alt="Synthetic demonstration of the four-service widget" width="380">

The preview uses synthetic test data. The installed app starts with **empty profiles
and no bundled activity**. TryHackMe uses a manually imported, dated snapshot.

## Install and configure

1. Install `ActivityQuadra-v1.1.1.apk` from Releases and open Activity Quadra.
2. The first launch asks for **your public usernames**. Enter at least one;
   leave services you do not use blank. No login, Google account or API key is needed.
3. The timezone defaults to your device timezone. Year `0` follows the current year.
4. Choose **Add widget → All four together**, or pick an individual service.
   If pinning is unavailable, long-press Home → Widgets → Activity Quadra.
5. Long-press the widget to resize it. Tap a graph for date details; tap a square,
   pinch to zoom, or use **Choose date** in the app.

Version 1.1 preserves usernames you explicitly saved in 1.0. The old author's
implicit defaults and bundled caches are no longer used. The app keeps its existing
package ID and release signing identity so official updates install over 1.0.

## Phones and tablets

Android 12+ widgets use the sizes reported by the launcher, including portrait and
landscape variants. Older or incompatible launchers use orientation-specific size
ranges. Wide combined widgets (at least 600dp × 300dp) arrange the graphs in a
**2 × 2** grid; narrow widgets stack the four graphs. The dashboard uses two columns
at window widths of at least 700dp and adapts again in split screen.

Graph cells stay square, including when resized. Android's launcher controls the
final grid spacing and permitted resize steps, so dimensions are not fixed to a
particular phone or tablet model. Automated rendering checks cover phone and tablet
portrait/landscape sizes. A physical Redmi Pad 2/HyperOS launcher check is still needed.
A widget image cannot show desktop hover tooltips; tap it for interactive date details.

## What the numbers mean

- **GitHub:** public contribution calendar and counts. GitHub's own calendar dates
  are preserved; no private repository access is requested.
- **Codeforces:** all submission attempts, including unsuccessful attempts, grouped
  by the chosen timezone. They are not distinct solved-problem counts. Requests are
  paginated with at least 2.2 seconds between pages; failed or truncated histories
  never replace the last successful cache with partial totals.
- **LeetCode:** **all-time unique solved / total available problems**, with Easy,
  Medium and Hard breakdowns. The heatmap and submission total cover the selected
  year and count submissions, not unique solved problems. Calendar dates stay in
  UTC as encoded by LeetCode. This uses its public profile GraphQL endpoint,
  without login or cookies; it is not a guaranteed stable third-party API.
- **TryHackMe:** a **dated public-profile snapshot** imported by the user. Automated
  requests were blocked/rate-limited, so the app does not retry that endpoint, sign
  in or read cookies. Exact daily counts are unavailable: dates show activity levels
  and the total is the profile's reported total. The snapshot retains its actual year.

### Refresh and offline use

Configured GitHub, Codeforces and LeetCode feeds refresh about every six hours while
at least one widget is present. Android battery restrictions can delay background
work. Refresh has cooldowns and server-limit backoff. Failures keep the last good
cache and its timestamp; missing/failed feeds are never presented as zero activity.
Blank usernames cause **no requests**. Removing every widget stops the periodic job.
Opening the app requests a refresh if the previous request was over an hour ago.
Cached data stays private and works offline. Changing profiles cannot show another
person's cached graph. TryHackMe never refreshes automatically.

### Import TryHackMe

Set your TryHackMe username in Profiles. Open `tryhackme-export.html` on your laptop
for the export instructions and `tryhackme-export.js` helper. The helper reads your
visible public graph without cookies or hidden APIs. Transfer the resulting JSON
and choose **Import snapshot**. Provider, username, full-year colors, capture time
and total are validated; failed imports preserve the old snapshot. Only the document
you select is read. Nothing is bundled for the author's profile.

## Privacy

No GitHub/Google tokens, OAuth clients, signing secrets or personal account settings
are included in the source or APK. Only your configured public usernames are sent
over HTTPS to the corresponding GitHub, Codeforces or LeetCode service, exposing
your IP address to that service as normal. Browser profile links use your browser.
Activity/settings stay in app-private storage; Android app backup is disabled.
TryHackMe imports are local. No Google Calendar, Tasks, Drive or notes access is used.

## Build and validate

Use JDK 21, Gradle 8.9, Android Gradle Plugin 8.7.3, Platform 35 and Build Tools 35.0.0.
The wrapper pins the official distribution SHA-256. Install Google's SDK packages
after accepting its SDK license, then set your local SDK path:

```sh
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
JAVA_HOME=/path/to/jdk-21 ./gradlew --no-daemon testDebugUnitTest lintRelease assembleRelease
```

Tests cover first-run empty profiles, optional services, cache isolation, public
LeetCode queries/count validation/UTC dates, GitHub parsing, Codeforces dates and
deduplication, TryHackMe imports, square-cell hit testing, refresh backoff, Android
APIs 28/35, exact-size widget options, and phone/tablet portrait/landscape rendering.
Synthetic screenshots are written to `app/build/reports/screenshots/`.
Optional local live-response tests use `ACTIVITY_TRIO_LIVE_FIXTURES` containing
`github.html`, `codeforces.json` and/or `leetcode.json`; those responses are not
bundled or committed. These host-side checks do not replace physical device testing.

To create a signed release:

```sh
JAVA_HOME=/path/to/jdk-21 ANDROID_HOME=/path/to/android-sdk ./build-release.sh
```

The helper keeps the signing identity outside the repo at
`~/.local/share/activity-trio/signing` (or `ACTIVITY_TRIO_SIGNING_DIR`), with private
permissions and passwords read from files. Back this directory up securely outside
Git to sign compatible future updates. A differently signed fork must be installed
separately after uninstalling the official app, which removes its settings.
Build output, SDK paths, tokens, caches and signing files are excluded from Git.

Official references: [Android widget sizing](https://developer.android.com/develop/ui/views/appwidgets/layouts),
[SDK Manager](https://developer.android.com/tools/sdkmanager),
[Codeforces API](https://codeforces.com/apiHelp/methods).

## License

Original app code: **MIT**, copyright 2026 0xSaadBinSultan. See [LICENSE](LICENSE),
[NOTICE](NOTICE) and [CONTRIBUTING.md](CONTRIBUTING.md). Upstream components keep their licenses.
