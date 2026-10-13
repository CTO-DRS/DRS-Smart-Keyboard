# DRS Smart Keyboard — :lib:compose shrinker rules (release/beta builds).
#
# This module hosts pure compose UI building blocks (scroll containers,
# app bars, cards, buttons, step wizard, resource lookups). Everything is
# statically reachable from Kotlin call sites: no reflection, no JNI, no
# dynamic class loading, and no serialization base classes declared here,
# so R8 needs no -keep rules from this module. The compose runtime and the
# jetpref material-ui dependency ship their own consumer rules; any app
# level keep requirements belong to the :app module configuration.
