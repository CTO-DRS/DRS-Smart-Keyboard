#!/bin/bash
# Re-provision the DRS build toolchain after an environment reset:
#   Android SDK (platform-34 + build-tools 34.0.0) -> /home/z/android-sdk
#   Kotlin 2.0.21 compiler                        -> /home/z/kotlinc
set -e
DL=/home/z/my-project/toolchain
mkdir -p "$DL"
cd "$DL"

echo "==> [1/5] Android cmdline-tools"
if [ ! -f cmdline-tools.zip ]; then
  curl -sSL --retry 3 -o cmdline-tools.zip \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
fi
ls -la cmdline-tools.zip

echo "==> [2/5] Unpack SDK"
rm -rf /home/z/android-sdk
mkdir -p /home/z/android-sdk/cmdline-tools
unzip -q cmdline-tools.zip -d /home/z/android-sdk/cmdline-tools
mv /home/z/android-sdk/cmdline-tools/cmdline-tools /home/z/android-sdk/cmdline-tools/latest

echo "==> [3/5] Accept licenses + install platform & build-tools"
export ANDROID_HOME=/home/z/android-sdk
yes | /home/z/android-sdk/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null 2>&1 || true
/home/z/android-sdk/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" > "$DL/sdkmanager.log" 2>&1
tail -3 "$DL/sdkmanager.log"

echo "==> [4/5] Kotlin 2.0.21"
if [ ! -f kotlin-compiler.zip ]; then
  curl -sSL --retry 3 -o kotlin-compiler.zip \
    https://github.com/JetBrains/kotlin/releases/download/v2.0.21/kotlin-compiler-2.0.21.zip
fi
rm -rf /home/z/kotlinc
unzip -q kotlin-compiler.zip -d /home/z

echo "==> [5/5] Verify"
/home/z/android-sdk/build-tools/34.0.0/aapt2 version
/home/z/kotlinc/bin/kotlinc -version 2>&1
ls /home/z/android-sdk/platforms/android-34/android.jar
echo "TOOLCHAIN READY"
