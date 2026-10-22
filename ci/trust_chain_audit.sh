#!/bin/bash
# DRS v1.2.x window — publisher trust-chain audit gate (seed cold storage).
#
# The v1.2.x roadmap KPI is «نقل بذرة الناشر ed25519 إلى تخزين بارد» —
# a provable custody property, not prose (ADR 0007 + docs/COLD_STORAGE.md).
# This gate proves, on every push:
#   1. no signing seed / private key material is tracked in the tree,
#   2. no such file was EVER committed across full git history,
#   3. the pinned publisher key (cto-drs-release-1) is well-formed and
#      pinned exactly once in DrsPackageTrust.kt,
#   4. every catalog entry carries the mandatory signature/pubkey_id pair
#      and each signature verifies against the pinned key (fail-closed),
#   5. the signing tool holds no seed literal beyond the public RFC 8032
#      §7.1 test vectors,
#   6. the external secret scan (gitleaks) is still declared in CI.
# It complements — never replaces — gitleaks, the privacy canary
# (ci/privacy_canary.sh) and the MASVS gate (ci/masvs_audit.sh).
#
# Exit 0 + "TRUST_CHAIN_AUDIT_PASS (n checks)" when every check holds;
# exit 1 + one "TRUST_CHAIN_AUDIT_FAIL: <what>" line per violation.
set -u
cd "$(dirname "$0")/.." || exit 1

pass=0
fail=0

ok() { pass=$((pass+1)); }
ko() { fail=$((fail+1)); echo "TRUST_CHAIN_AUDIT_FAIL: $1"; }

TRUST=app/src/main/kotlin/com/drs/smartkeyboard/lib/ext/DrsPackageTrust.kt
MANIFEST=packages/manifest.json
SIGN_TOOL=tools/drs_package_sign.py
WF=.github/workflows/security.yml

# --- Gate 1: no key material tracked in the working tree ---------------
# .local-secrets/ is git-ignored BY DESIGN (the seed's only warm home);
# its local existence on a signing machine is expected — what must fail
# is git ever TRACKING it, or any *.seed/*.pem/*.jks/*.keystore file.
tracked_bad=$(git ls-files | grep -E '(^|/)\.local-secrets/|\.seed$|\.pem$|\.jks$|\.keystore$' || true)
if [ -z "$tracked_bad" ]; then
  ok
else
  ko "key material tracked in git: $tracked_bad"
fi

# --- Gate 2: full history never ADDED such a file ----------------------
# fetch-depth: 0 in CI gives the full history; locally this still covers
# every commit the clone knows about.
hist_bad=$(git log --all --diff-filter=A --name-only --format= | grep -E '(^|/)\.local-secrets/|\.seed$|\.pem$|\.jks$|\.keystore$' | sort -u || true)
if [ -z "$hist_bad" ]; then
  ok
else
  ko "key material was committed in history: $hist_bad"
fi

# --- Gate 3: pinned key well-formed, pinned exactly once ---------------
# Only the pinned CONSTANT counts (KDoc prose may legitimately name the id).
key_id_count=$(grep -c 'const val RELEASE_KEY_ID = "cto-drs-release-1"' "$TRUST" || true)
if [ "$key_id_count" -eq 1 ]; then
  ok
else
  ko "pinned key constant expected exactly once, found $key_id_count"
fi

pubkey=$(grep -oE '"[0-9a-f]{64}"' "$TRUST" | head -1 | tr -d '"')
if [ ${#pubkey} -eq 64 ]; then
  ok
else
  ko "pinned publisher pubkey missing or malformed in $TRUST"
fi

pub_lines=$(grep -cE '"[0-9a-f]{64}"' "$TRUST" || true)
if [ "$pub_lines" -eq 1 ]; then
  ok
else
  ko "expected exactly one pinned key literal, found $pub_lines"
fi

# --- Gate 4: catalog signatures mandatory and valid (fail-closed) ------
if [ -f "$MANIFEST" ]; then
  py_out=$(python3 - "$MANIFEST" "$pubkey" "$SIGN_TOOL" <<'PYEOF'
import json, subprocess, sys

manifest_path, pubkey, tool = sys.argv[1], sys.argv[2], sys.argv[3]
data = json.load(open(manifest_path, encoding="utf-8"))
entries = data.get("packages", [])
if not entries:
    print("KO catalog has no entries")
    sys.exit(0)

pairless = [e.get("package_id", "?") for e in entries
            if not e.get("signature") or not e.get("pubkey_id")]
if pairless:
    print("KO entries missing signature/pubkey_id pair: " + ", ".join(pairless))
    sys.exit(0)

bad_ids = [e.get("pubkey_id") for e in entries if e.get("pubkey_id") != "cto-drs-release-1"]
if bad_ids:
    print("KO entries signed by unknown keys: " + ", ".join(bad_ids))
    sys.exit(0)

import os, re
invalid = []
checked = 0
for e in entries:
    tail = e["package_id"].split(".")[-1]
    path = os.path.join("packages", f"pkg-{tail}-v{e['version']}.flex")
    if not os.path.exists(path):
        print("KO referenced package asset missing from repo: " + path)
        sys.exit(0)
    r = subprocess.run(["python3", tool, "verify", path, e["signature"], "--pubkey", pubkey],
                       capture_output=True, text=True)
    checked += 1
    if r.returncode != 0:
        invalid.append(path)

if invalid:
    print("KO signatures failed RFC 8032 verification: " + ", ".join(invalid))
else:
    print("OK " + str(checked))
PYEOF
)
  case "$py_out" in
    OK*) ok ;;  # "OK n" = n catalog entries pair-checked and RFC-verified
    KO*) ko "${py_out#KO }" ;;
    *)   ko "catalog audit crashed: $py_out" ;;
  esac
else
  ko "$MANIFEST missing"
fi

# --- Gate 5: sign tool holds no seed beyond the RFC 8032 vector block ---
# Structural rule, not an allowlist of values: every 64-hex literal in the
# signing tool must live INSIDE the official RFC_VECTORS table (public
# TEST 1–3 constants). Any seed pasted elsewhere = immediate violation.
stray=$(python3 - "$SIGN_TOOL" <<'PYEOF'
import re, sys

src = open(sys.argv[1], encoding="utf-8").read()
start = src.find("RFC_VECTORS = [")
end = src.find("\n]", start)
if start < 0 or end < 0:
    print("RFC_VECTORS table not found — tool integrity unknown")
    sys.exit(0)
block = src[start:end]
strays = [h for h in re.findall(r"[0-9a-f]{64}", src)
          if h not in block]
if strays:
    print("STRAY " + " ".join(sorted(set(strays))))
PYEOF
)
case "$stray" in
  "") ok ;;
  STRAY*) ko "64-hex literal outside the RFC 8032 vector table (leaked seed?): $stray" ;;
  *)      ko "$stray" ;;
esac

# --- Gate 6: external secret scan still declared in CI -----------------
if grep -q "gitleaks" "$WF"; then
  ok
else
  ko "gitleaks external scan missing from $WF"
fi

# --- Verdict ------------------------------------------------------------
total=$((pass+fail))
if [ "$fail" -eq 0 ]; then
  echo "TRUST_CHAIN_AUDIT_PASS ($pass checks)"
  exit 0
else
  echo "TRUST_CHAIN_AUDIT: $fail violation(s) across $total checks"
  exit 1
fi
