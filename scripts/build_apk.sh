#!/bin/bash
# DRS Smart Keyboard — manual APK build pipeline (no Gradle needed).
# aapt2 -> kotlinc -> d8 -> zipalign -> apksigner
set -e

PROJECT=/home/z/my-project/DRSKeyboard
SDK=/home/z/android-sdk
BT=$SDK/build-tools/34.0.0
AJAR=$SDK/platforms/android-34/android.jar
KOTLINC=/home/z/kotlinc/bin/kotlinc
KSTDLIB=/home/z/kotlinc/lib/kotlin-stdlib.jar
WORK=$PROJECT/build/manual
OUT=/home/z/my-project/download

rm -rf "$WORK"
mkdir -p "$WORK/gen" "$WORK/classes" "$OUT"

echo "==> [1/7] aapt2 compile resources"
"$BT/aapt2" compile --dir "$PROJECT/app/src/main/res" -o "$WORK/res.zip"

# aapt2 requires the legacy package attribute (AGP supplies it from build.gradle
# namespace, so the source manifest stays AGP-8-clean; inject it only here).
sed 's/<manifest /<manifest package="com.drs.keyboard" /' \
  "$PROJECT/app/src/main/AndroidManifest.xml" > "$WORK/AndroidManifest.xml"

echo "==> [2/7] aapt2 link"
"$BT/aapt2" link \
  -o "$WORK/unsigned.apk" \
  -I "$AJAR" \
  --manifest "$WORK/AndroidManifest.xml" \
  --java "$WORK/gen" \
  -A "$PROJECT/app/src/main/assets" \
  --min-sdk-version 26 \
  --target-sdk-version 34 \
  --version-code 14 \
  --version-name 1.14.0 \
  --auto-add-overlay \
  "$WORK/res.zip"

echo "==> [3/7] kotlinc compile"
"$KOTLINC" \
  -cp "$AJAR" \
  -d "$WORK/classes" \
  "$PROJECT/app/src/main/java" "$WORK/gen" > "$WORK/kotlinc.log" 2>&1
KSTATUS=$?
grep -E "error:|error :" "$WORK/kotlinc.log" | head -60 || true
if [ $KSTATUS -ne 0 ]; then
  echo "!!! KOTLINC FAILED (exit $KSTATUS)"
  tail -20 "$WORK/kotlinc.log"
  exit 1
fi
if [ -z "$(find "$WORK/classes" -name '*.class' 2>/dev/null | head -1)" ]; then
  echo "!!! NO CLASS FILES PRODUCED"
  exit 1
fi

echo "==> [4/7] d8 dex"
"$BT/d8" \
  --release \
  --lib "$AJAR" \
  --min-api 26 \
  --output "$WORK" \
  $(find "$WORK/classes" -name "*.class") \
  "$KSTDLIB" 2>&1 | tail -5
if [ ! -f "$WORK/classes.dex" ]; then
  echo "!!! DEX NOT PRODUCED"
  exit 1
fi

echo "==> [5/7] package dex into apk"
cd "$WORK"
cp unsigned.apk base.apk
zip -q -j base.apk classes.dex

echo "==> [6/7] zipalign"
"$BT/zipalign" -f 4 base.apk aligned.apk

echo "==> [7/7] sign"
# stable keystore location: persists across builds so the release signature
# never changes (users can upgrade without uninstalling)
KS="$PROJECT/build/drs.keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias drs -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass drskeyboard -keypass drskeyboard \
    -dname "CN=DRS Smart Keyboard, OU=DRS, O=DRS, L=Riyadh, C=SA" >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:drskeyboard \
  --key-pass pass:drskeyboard --out "$OUT/DRS-Smart-Keyboard-v1.14.0.apk" aligned.apk
"$BT/apksigner" verify --print-certs "$OUT/DRS-Smart-Keyboard-v1.14.0.apk" | head -5

ls -la "$OUT/DRS-Smart-Keyboard-v1.14.0.apk"
echo "==> DONE"
