#!/bin/sh
# Reproducible build of the side-by-side subtitle test APK. Written by review; NOT executed by the reviewer.
# Inputs come from the environment so no tool path or secret is stored here:
#   APKTOOL       apktool launcher (3.0.3 was used before)
#   ANDROID_JAR   platform android.jar used to compile the helper classes
#   BUILD_TOOLS   Android build-tools directory containing d8, zipalign, apksigner
#   KEYSTORE      local test keystore (never the official Emby key, never committed)
#   KEY_ALIAS     key alias in KEYSTORE
#   KS_PASS_ENV   name of the environment variable holding the keystore password (passed as env:NAME)
set -eu
cd "$(dirname "$0")"
: "${APKTOOL:?}" "${ANDROID_JAR:?}" "${BUILD_TOOLS:?}" "${KEYSTORE:?}" "${KEY_ALIAS:?}" "${KS_PASS_ENV:?}"

EXPECTED=0246821852b9a7b6e1448f751601c0040c1f3a9de80c4ea778fb2ba67a16d147
ACTUAL=$(shasum -a 256 original.apk | cut -d' ' -f1)
[ "$ACTUAL" = "$EXPECTED" ] || { echo "original.apk digest mismatch: $ACTUAL" >&2; exit 1; }

rm -rf build/decoded build/classes build/dex build/unsigned.apk build/aligned.apk
mkdir -p build/classes build/dex
"$APKTOOL" d -f -o build/decoded original.apk
python3 -I patch.py build/decoded

# Approved TV banner; preserves the playback implementation.
cp branding/app_icon.png build/decoded/res/drawable/app_icon.png

# Helper classes: only android.jar on the classpath, never tests/stubs.
javac --release 8 -classpath "$ANDROID_JAR" -d build/classes src/local/emby/subtitles/*.java
"$BUILD_TOOLS/d8" --release --min-api 24 --lib "$ANDROID_JAR" --output build/dex $(find build/classes -name '*.class')

"$APKTOOL" b -o build/unsigned.apk build/decoded
# Append the helper DEX as the next classesN.dex so native multidex (minSdk 24) loads it.
COUNT=$(unzip -Z1 build/unsigned.apk | grep -c '^classes[0-9]*\.dex$')
NEXT=$((COUNT + 1))
cp build/dex/classes.dex "build/classes$NEXT.dex"
(cd build && zip -q -X unsigned.apk "classes$NEXT.dex" && rm "classes$NEXT.dex")

"$BUILD_TOOLS/zipalign" -p -f 4 build/unsigned.apk build/aligned.apk
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$KEY_ALIAS" --ks-pass "env:$KS_PASS_ENV" \
  --out build/emby-subtitle-test.apk build/aligned.apk
"$BUILD_TOOLS/apksigner" verify --print-certs build/emby-subtitle-test.apk
unzip -Z1 build/emby-subtitle-test.apk | grep '^classes[0-9]*\.dex$'
echo "built build/emby-subtitle-test.apk (helper dex: classes$NEXT.dex)"
