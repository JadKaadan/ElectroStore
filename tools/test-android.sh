#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
project_dir="$PWD"
: "${ANDROID_HOME:?Set ANDROID_HOME}"
: "${BUILD_TOOLS:?Set BUILD_TOOLS to an installed Android build-tools folder}"
: "${ECJ:?Set ECJ to the Eclipse Java compiler jar}"
test_dir="$project_dir/build/regression"
mkdir -p "$test_dir/classes" "$test_dir/dex" "$test_dir/gen"
cat > "$test_dir/AndroidManifest.xml" <<'XML'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.electrostore.app.test">
<uses-sdk android:minSdkVersion="21" android:targetSdkVersion="34"/>
<application android:label="ElectroStore regression tests" android:debuggable="true"/>
<instrumentation android:name="com.electrostore.app.RegressionRunner" android:targetPackage="com.electrostore.app"/>
</manifest>
XML
"$BUILD_TOOLS/aapt2" link -o "$test_dir/test-base.apk" -I "$ANDROID_HOME/platforms/android-34/android.jar" --manifest "$test_dir/AndroidManifest.xml"
find app/src/androidTest/java -name '*.java' > "$test_dir/sources.txt"
java -jar "$ECJ" -source 8 -target 8 -nowarn -encoding UTF-8 -bootclasspath "$ANDROID_HOME/platforms/android-34/android.jar:$BUILD_TOOLS/core-lambda-stubs.jar" \
 -classpath "$project_dir/build/standalone/classes" -d "$test_dir/classes" @"$test_dir/sources.txt"
find "$test_dir/classes" -name '*.class' > "$test_dir/classes.txt"
java -cp "$BUILD_TOOLS/lib/d8.jar" com.android.tools.r8.D8 --lib "$ANDROID_HOME/platforms/android-34/android.jar" --min-api 21 --output "$test_dir/dex" @"$test_dir/classes.txt"
python3 - "$test_dir" <<'PY'
import sys,zipfile,shutil
from pathlib import Path
p=Path(sys.argv[1]);shutil.copyfile(p/'test-base.apk',p/'test-unsigned.apk')
with zipfile.ZipFile(p/'test-unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for f in (p/'dex').glob('*.dex'):z.write(f,f.name)
PY
"$BUILD_TOOLS/zipalign" -f 4 "$test_dir/test-unsigned.apk" "$test_dir/test-aligned.apk"
: "${ELECTROSTORE_KEYSTORE_PATH:?Set ELECTROSTORE_KEYSTORE_PATH to a locally stored signing key}"
: "${ELECTROSTORE_KEYSTORE_PASSWORD:?Set ELECTROSTORE_KEYSTORE_PASSWORD}"
: "${ELECTROSTORE_KEY_ALIAS:?Set ELECTROSTORE_KEY_ALIAS}"
: "${ELECTROSTORE_KEY_PASSWORD:?Set ELECTROSTORE_KEY_PASSWORD}"
export ELECTROSTORE_KEYSTORE_PASSWORD ELECTROSTORE_KEY_PASSWORD
java -jar "$BUILD_TOOLS/lib/apksigner.jar" sign --ks "$ELECTROSTORE_KEYSTORE_PATH" --ks-key-alias "$ELECTROSTORE_KEY_ALIAS" --ks-pass env:ELECTROSTORE_KEYSTORE_PASSWORD --key-pass env:ELECTROSTORE_KEY_PASSWORD --out "$test_dir/regression.apk" "$test_dir/test-aligned.apk"
echo "Built emulator-only tests: $test_dir/regression.apk"
