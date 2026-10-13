# DRS Smart Keyboard — :lib:android shrinker rules (release/beta builds).
#
# This module is a thin, statically-resolved wrapper layer over platform
# services (settings, vibrator, clipboard, assets, package manager). It
# exposes no reflection, JNI, or dynamic-class-loading surface that R8
# cannot trace, so no -keep rules are required here. The serialization
# runtime ships its own bundled consumer rules, and any app-level keep
# requirements belong to the :app module configuration, not this library.
