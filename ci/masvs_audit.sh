#!/bin/bash
# DRS v1.4.x security wave — internal MASVS audit gate (automated subset).
#
# The v1.4.x roadmap KPI is «اجتياز MASVS-PRIVACY داخليًا» — a real
# pass, not prose. This gate runs the machine-checkable subset of the
# internal audit (docs/MASVS_AUDIT.md owns the full control table with
# evidence per control); it complements — never replaces — the privacy
# canary (ci/privacy_canary.sh) and the manifest-contract unit tests
# (DrsP3PrivacyTests / DrsSecurityWaveTests).
#
# Exit 0 + "MASVS_AUDIT_PASS (n checks)" when every check holds;
# exit 1 + one "MASVS_FAIL: <what>" line per violation otherwise.
set -u
cd "$(dirname "$0")/.." || exit 1

M=app/src/main/AndroidManifest.xml
NSC=app/src/main/res/xml/network_security_config.xml

pass=0
fail=0

ok() { pass=$((pass+1)); }
ko() { fail=$((fail+1)); echo "MASVS_FAIL: $1"; }

# --- Strip XML comments once; assertions target live XML, not prose.
stripped=$(sed 's/<!--/\n&/g; s/-->/&\n/g' "$M" | sed '/<!--/,/-->/d')

count_uses_permission() { rg -c "uses-permission android:name=" "$M" || echo 0; }

# MASVS-PLATFORM-1 / MASVS-PRIVACY-1: the five-permission allowlist.
n=$(count_uses_permission)
if [ "$n" -eq 5 ]; then ok; else ko "manifest requests $n permissions, the allowlist is exactly 5"; fi

for p in VIBRATE RECORD_AUDIO POST_NOTIFICATIONS INTERNET REQUEST_INSTALL_PACKAGES; do
  if rg -q "uses-permission android:name=\"android.permission.$p\"" "$M"; then
    ok
  else
    ko "expected permission $p missing from the manifest"
  fi
done

for p in ACCESS_NETWORK_STATE ACCESS_WIFI_STATE ACCESS_FINE_LOCATION CAMERA \
         READ_CONTACTS READ_PHONE_STATE READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do
  if rg -q "uses-permission android:name=\"android.permission.$p\"" "$M"; then
    ko "forbidden permission $p present in the manifest"
  else
    ok
  fi
done
if rg -q "QUERY_ALL_PACKAGES" "$M"; then ko "QUERY_ALL_PACKAGES present"; else ok; fi

# MASVS-STORAGE-1: no cloud backup of user data.
if rg -q 'android:allowBackup="false"' "$M"; then ok; else ko 'allowBackup is not false'; fi
if rg -q 'android:dataExtractionRules=' "$M"; then ok; else ko 'dataExtractionRules not wired'; fi
if rg -q 'android:fullBackupContent=' "$M"; then ok; else ko 'fullBackupContent not wired'; fi
for f in backup_rules.xml data_extraction_rules.xml network_security_config.xml; do
  if [ -f "app/src/main/res/xml/$f" ]; then ok; else ko "res/xml/$f missing"; fi
done

# MASVS-NETWORK-1/2: explicit cleartext denial, system trust anchors only.
if rg -q '<base-config cleartextTrafficPermitted="false">' "$NSC"; then ok; else ko 'explicit cleartext denial missing from base-config'; fi
if rg -q 'cleartextTrafficPermitted="true"' "$NSC" "$M"; then ko 'cleartextTrafficPermitted=true present'; else ok; fi
if rg -q 'certificates src="user' "$NSC"; then ko 'user trust anchors present (system only expected)'; else ok; fi

# MASVS-PLATFORM-2/3: exported surface stays on the documented allowlist.
if echo "$stripped" | rg -q "<profileable"; then ko "profileable present in live XML"; else ok; fi

# Robust exported-component walk: awk over component tags, capturing
# name/exported from the OPENING tag only (children intent-filters
# cannot pollute the identity).
true_names=$(awk '
  /<(service|activity-alias|activity|receiver|provider)([ \t>]|$)/ { incomp=1; cname=""; cexp="" }
  incomp && /android:name="/ && cname=="" {
    match($0, /android:name="[^"]*"/); cname=substr($0, RSTART+14, RLENGTH-15)
  }
  incomp && /android:exported="true"/ { cexp="true" }
  incomp && />/ {
    if (cexp=="true") print cname; incomp=0
  }
' "$M")
for name in $true_names; do
  case "$name" in
    com.drs.smartkeyboard.DrsImeService|\
    com.drs.smartkeyboard.DrsSpellCheckerService|\
    com.drs.smartkeyboard.app.DrsAppActivity|\
    com.drs.smartkeyboard.SettingsLauncherAlias|\
    com.drs.smartkeyboard.ime.clipboard.DrsCopyToClipboardActivity)
      ok
      ;;
    *)
      ko "unexpected exported component: $name"
      ;;
  esac
done
# The allowlist must be fully exercised (five exported components).
exported_count=$(printf '%s\n' "$true_names" | rg -c . || echo 0)
if [ "$exported_count" -eq 5 ]; then ok; else ko "exported components = $exported_count, expected 5"; fi

# Platform binding permissions on the exported services.
if rg -q 'android.permission.BIND_INPUT_METHOD' "$M"; then ok; else ko 'IME service lacks BIND_INPUT_METHOD'; fi
if rg -q 'android.permission.BIND_TEXT_SERVICE' "$M"; then ok; else ko 'spell-checker service lacks BIND_TEXT_SERVICE'; fi

# MASVS-RESILIENCE-1 (partial, automated slice): release builds keep
# default debuggable=false — no explicit debuggable=true anywhere.
if rg -q 'android:debuggable="true"' "$M"; then ko 'debuggable=true present'; else ok; fi

echo "---"
if [ "$fail" -eq 0 ]; then
  echo "MASVS_AUDIT_PASS ($pass checks)"
  exit 0
else
  echo "MASVS_AUDIT_FAIL ($fail failures / $pass passed)"
  exit 1
fi
