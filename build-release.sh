#!/usr/bin/env bash
# Build/test first. Keep the release signing identity private and outside source.
set -euo pipefail
project_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
: "${JAVA_HOME:?Set JAVA_HOME to your JDK 21 directory}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK directory}"
export PATH="$JAVA_HOME/bin:$PATH"
"$project_dir/gradlew" -p "$project_dir" --no-daemon testDebugUnitTest lintRelease assembleRelease
signing_dir="${ACTIVITY_TRIO_SIGNING_DIR:-$HOME/.local/share/activity-trio/signing}"
umask 077
mkdir -p -- "$signing_dir" "$project_dir/dist"
chmod 700 -- "$signing_dir"
if [[ ! -f "$signing_dir/release.jks" ]]; then
    # Preserve any existing password file rather than silently replacing it.
    if [[ ! -f "$signing_dir/password" ]]; then
        python3 -c 'import secrets; print(secrets.token_urlsafe(48))' > "$signing_dir/password"
    fi
    "$JAVA_HOME/bin/keytool" -genkeypair -keystore "$signing_dir/release.jks" -storetype PKCS12 \
        -alias activity-trio -keyalg RSA -keysize 3072 -validity 10000 \
        -dname 'CN=Activity Trio, OU=Personal Apps' \
        -storepass:file "$signing_dir/password" -keypass:file "$signing_dir/password"
fi
[[ -f "$signing_dir/password" ]] || { echo 'Missing local signing-password file.' >&2; exit 1; }
chmod 600 -- "$signing_dir/release.jks" "$signing_dir/password"
tools_dir="$ANDROID_HOME/build-tools/35.0.0"
"$tools_dir/zipalign" -f -p 4 "$project_dir/app/build/outputs/apk/release/app-release-unsigned.apk" "$project_dir/dist/aligned.apk"
"$tools_dir/apksigner" sign --ks "$signing_dir/release.jks" --ks-key-alias activity-trio \
    --ks-pass "file:$signing_dir/password" \
    --out "$project_dir/dist/ActivityTrio-v1.1.0.apk" "$project_dir/dist/aligned.apk"
"$tools_dir/apksigner" verify --verbose "$project_dir/dist/ActivityTrio-v1.1.0.apk"
rm -- "$project_dir/dist/aligned.apk"
(cd -- "$project_dir/dist" && sha256sum ActivityTrio-v1.1.0.apk > ActivityTrio-v1.1.0.apk.sha256)
printf 'Signed APK: %s\n' "$project_dir/dist/ActivityTrio-v1.1.0.apk"
