# DRS Smart Keyboard keeps release builds simple: no reflection tricks,
# no JNI, no external SDKs. Default optimizations are enough.
-dontwarn org.jetbrains.annotations.**
-keep class com.drs.keyboard.ime.** { *; }
