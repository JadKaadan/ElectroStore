#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
project_dir="$PWD"
main_dir="$project_dir/app/src/main"
build_dir="$project_dir/build/standalone"
sdk_dir="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}"
build_tools="${BUILD_TOOLS:-$(ls -d "$sdk_dir"/build-tools/* 2>/dev/null | sort -V | tail -1 || true)}"
: "${AAPT2:=$build_tools/aapt2}"
: "${ANDROID_JAR:=$sdk_dir/platforms/android-34/android.jar}"
: "${D8:=$build_tools/lib/d8.jar}"
: "${APKSIGNER:=$build_tools/lib/apksigner.jar}"
: "${ZIPALIGN:=$build_tools/zipalign}"
: "${ELECTROSTORE_KEYSTORE_PATH:?Set ELECTROSTORE_KEYSTORE_PATH to your private keystore}"
: "${ELECTROSTORE_KEYSTORE_PASSWORD:?Set ELECTROSTORE_KEYSTORE_PASSWORD}"
: "${ELECTROSTORE_KEY_ALIAS:?Set ELECTROSTORE_KEY_ALIAS}"
: "${ELECTROSTORE_KEY_PASSWORD:?Set ELECTROSTORE_KEY_PASSWORD}"
KEYSTORE="$ELECTROSTORE_KEYSTORE_PATH"
KS_PASS="$ELECTROSTORE_KEYSTORE_PASSWORD"
KEY_ALIAS="$ELECTROSTORE_KEY_ALIAS"
KEY_PASS="$ELECTROSTORE_KEY_PASSWORD"
app_version="$(python3 -c 'import sys,xml.etree.ElementTree as E; print(E.parse(sys.argv[1]).getroot().attrib["{http://schemas.android.com/apk/res/android}versionName"])' "$main_dir/AndroidManifest.xml")"
: "${OUTPUT_APK:=$project_dir/ElectroStore-v$app_version.apk}"
for file in "$AAPT2" "$ANDROID_JAR" "$D8" "$APKSIGNER" "$ZIPALIGN" "$KEYSTORE"; do
  [ -f "$file" ] || { echo "Missing SDK tool or signing key: $file"; exit 1; }
done
mkdir -p "$build_dir"
rm -rf "$build_dir/gen" "$build_dir/classes" "$build_dir/dex"
mkdir -p "$build_dir/gen" "$build_dir/classes" "$build_dir/dex"
sed 's|<manifest |<manifest package="com.electrostore.app" |' "$main_dir/AndroidManifest.xml" > "$build_dir/AndroidManifest.xml"
"$AAPT2" compile --dir "$main_dir/res" -o "$build_dir/res.zip"
debug_args=()
if [ "${APP_DEBUGGABLE:-0}" = 1 ]; then debug_args+=(--debug-mode); fi
"$AAPT2" link -o "$build_dir/base.apk" -I "$ANDROID_JAR" --manifest "$build_dir/AndroidManifest.xml" \
  --java "$build_dir/gen" --min-sdk-version 21 --target-sdk-version 34 --no-version-vectors \
  "${debug_args[@]}" "$build_dir/res.zip"
find "$main_dir/java" "$build_dir/gen" -name '*.java' > "$build_dir/sources.txt"
if [ -n "${ECJ:-}" ]; then
  java -jar "$ECJ" -source 8 -target 8 -nowarn -encoding UTF-8 -bootclasspath "$ANDROID_JAR:$build_tools/core-lambda-stubs.jar" -d "$build_dir/classes" @"$build_dir/sources.txt"
else
  javac -source 8 -target 8 -nowarn -encoding UTF-8 -bootclasspath "$ANDROID_JAR:$build_tools/core-lambda-stubs.jar" -d "$build_dir/classes" @"$build_dir/sources.txt"
fi
find "$build_dir/classes" -name '*.class' > "$build_dir/classes.txt"
java -cp "$D8" com.android.tools.r8.D8 --lib "$ANDROID_JAR" --min-api 21 --output "$build_dir/dex" @"$build_dir/classes.txt"
python3 - "$build_dir" <<'PY'
from pathlib import Path
import sys, shutil, zipfile
b=Path(sys.argv[1]);shutil.copyfile(b/'base.apk',b/'unaligned.apk')
with zipfile.ZipFile(b/'unaligned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for p in (b/'dex').glob('*.dex'):z.write(p,p.name)
PY
"$ZIPALIGN" -f -p 4 "$build_dir/unaligned.apk" "$build_dir/aligned.apk"
export KS_PASS KEY_PASS
java -jar "$APKSIGNER" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" --ks-pass env:KS_PASS --key-pass env:KEY_PASS \
  --out "$OUTPUT_APK" "$build_dir/aligned.apk"
java -jar "$APKSIGNER" verify "$OUTPUT_APK"
"$ZIPALIGN" -c 4 "$OUTPUT_APK"
echo "Built and verified: $OUTPUT_APK"
