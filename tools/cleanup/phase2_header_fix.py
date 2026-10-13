#!/usr/bin/env python3
"""Phase 2 Part 1 — legal header fix for the 5 files ruled derived (owner
decision: option A).

  file                        header source
  MimeTypeFilter.kt           literal header of floris@fe1241f MimeTypeFilter.kt
  JetPref.kt                  literal header of jetpref@7a3df4e jvmCommon JetPref.kt
  DrsAppBar.kt                literal header of floris@fe1241f FlorisAppBar.kt
                              (fuzzy-derived file, Phase-1 treatment)
  JetPrefJvm.kt               jetpref CANONICAL header (merged from 2 sources)
  PreferenceDataJvm.kt        jetpref CANONICAL header (merged from 2 sources)

Guarantee: SHA-256 of the body (everything after the license header, header
and leading blank lines stripped) is identical before == after. Only the
header block is replaced. Dry-run by default; --apply writes.

Also patches:
  - canonical_verdicts.csv: JetPrefJvm/PreferenceDataJvm reclassified
    drs-original -> slightly-modified with refs + measurements (owner ruling)
  - phase1_header_map.csv: the 5 rows updated to phase2-restore actions
  - measurements/phase2_header_fix.csv: full audit trail (file | ref | kind |
    body sha before | body sha after)
"""
from __future__ import annotations

import argparse
import collections
import csv
import hashlib
import os
import sys
from pathlib import Path

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from restore_headers import extract_header, body_after_header, iter_sources  # noqa: E402

MEAS = os.path.join(HERE, "measurements")
FLORIS_HASH = "fe1241f4921b3eae923571ff7a3e113a7e10d677"
JETPREF_HASH = "7a3df4eb76ff81245244761f6c160eab31e9f73f"

FIXES = [
    {
        "file": "lib/kotlin/src/main/kotlin/org/drs/lib/kotlin/MimeTypeFilter.kt",
        "up": ("floris", "lib/kotlin/src/main/kotlin/org/florisboard/lib/kotlin/MimeTypeFilter.kt"),
        "kind": "literal-header (100.0% match)",
        "ref": f"floris@{FLORIS_HASH}:lib/kotlin/src/main/kotlin/org/florisboard/lib/kotlin/MimeTypeFilter.kt",
    },
    {
        "file": "lib/jetpref/datastore-model/src/main/kotlin/org/drs/jetpref/datastore/JetPref.kt",
        "up": ("jetpref", "datastore-model/src/jvmCommonMain/kotlin/dev/patrickgold/jetpref/datastore/JetPref.kt"),
        "kind": "literal-header (100.0% match)",
        "ref": f"jetpref@{JETPREF_HASH}:datastore-model/src/jvmCommonMain/kotlin/dev/patrickgold/jetpref/datastore/JetPref.kt",
    },
    {
        "file": "lib/compose/src/main/kotlin/org/drs/lib/compose/DrsAppBar.kt",
        "up": ("floris", "lib/compose/src/main/kotlin/org/florisboard/lib/compose/FlorisAppBar.kt"),
        "kind": "literal-header (fuzzy-derived 44.4%, phase-1 treatment)",
        "ref": f"floris@{FLORIS_HASH}:lib/compose/src/main/kotlin/org/florisboard/lib/compose/FlorisAppBar.kt",
    },
    {
        "file": "lib/jetpref/datastore-model/src/main/kotlin/org/drs/jetpref/datastore/JetPrefJvm.kt",
        "up": None,
        "kind": "canonical-header (merged from two sources)",
        "ref": f"jetpref-canonical@{JETPREF_HASH}",
    },
    {
        "file": "lib/jetpref/datastore-model/src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceDataJvm.kt",
        "up": None,
        "kind": "canonical-header (merged from two sources)",
        "ref": f"jetpref-canonical@{JETPREF_HASH}",
    },
]

RECLASS = [
    {
        "file": "lib/jetpref/datastore-model/src/main/kotlin/org/drs/jetpref/datastore/JetPrefJvm.kt",
        "verdict": "slightly-modified",
        "upstream_ref": f"jetpref@{JETPREF_HASH}:datastore-model/src/jvmCommonMain/kotlin/dev/patrickgold/jetpref/datastore/JetPref.kt",
        "similarity_pct": "95.2",
        "note": "phase2 reclass per owner ruling (option A); was drs-original (phase1 scan gap); best-match scan documented in phase2_review.csv",
    },
    {
        "file": "lib/jetpref/datastore-model/src/main/kotlin/org/drs/jetpref/datastore/model/PreferenceDataJvm.kt",
        "verdict": "slightly-modified",
        "upstream_ref": f"jetpref@{JETPREF_HASH}:datastore-model/src/jvmCommonMain/kotlin/dev/patrickgold/jetpref/datastore/model/PreferenceData.kt",
        "similarity_pct": "93.2",
        "note": "phase2 reclass per owner ruling (option A); was drs-original (phase1 scan gap); best-match scan documented in phase2_review.csv",
    },
]


def sha256_text(t: str) -> str:
    return hashlib.sha256(t.encode("utf-8")).hexdigest()


def canonical_header(root: Path) -> str:
    """Most-voted header across an upstream repo (same logic as Phase 1)."""
    votes: collections.Counter = collections.Counter()
    for p in iter_sources(root, (".kt", ".kts")):
        raw = p.read_text(encoding="utf-8", errors="replace")
        h = extract_header(raw)
        if h:
            votes[h] += 1
    return votes.most_common(1)[0][0]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(HERE, "..", "..")))
    ap.add_argument("--floris", default="/home/z/my-project/repos/floris-main")
    ap.add_argument("--jetpref", default="/home/z/my-project/repos/jetpref-main")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()

    repo = Path(args.repo)
    roots = {"floris": Path(args.floris), "jetpref": Path(args.jetpref)}
    jetpref_canonical = canonical_header(roots["jetpref"])
    print(f"jetpref canonical header votes: using most-common (Copyright 2025 "
          f"Patrick Goldinger expected) -> first line: "
          f"{jetpref_canonical.splitlines()[1] if len(jetpref_canonical.splitlines()) > 1 else '?'}")

    out_rows = []
    failures = 0
    for fx in FIXES:
        ours = repo / fx["file"]
        raw = ours.read_text(encoding="utf-8")
        old_header = extract_header(raw)
        if "DRS Smart Keyboard" not in old_header:
            print(f"FAIL {fx['file']}: current header is not a DRS placeholder "
                  f"(refusing to touch)")
            failures += 1
            continue
        body = body_after_header(raw)
        sha_before = sha256_text(body)
        if fx["up"] is None:
            new_header = jetpref_canonical
        else:
            up_raw = (roots[fx["up"][0]] / fx["up"][1]).read_text(encoding="utf-8")
            new_header = extract_header(up_raw)
            if not new_header.startswith("/*") or "Apache" not in new_header:
                print(f"FAIL {fx['file']}: upstream header extraction failed")
                failures += 1
                continue
        raw_after = new_header + "\n\n" + body
        body_after = body_after_header(raw_after)
        sha_after = sha256_text(body_after)
        ok = sha_before == sha_after
        if not ok:
            failures += 1
        tag = "PASS" if ok else "FAIL"
        print(f"{tag} {fx['file']}\n     header={fx['kind']}\n     body sha256 "
              f"before={sha_before[:16]}.. after={sha_after[:16]}.. equal={ok}")
        out_rows.append({"file": fx["file"], "upstream_ref": fx["ref"],
                         "kind": fx["kind"], "body_sha256": sha_before})
        if args.apply and ok:
            ours.write_text(raw_after, encoding="utf-8", newline="")

    print(f"\nheader fixes: {len(FIXES) - failures}/{len(FIXES)} ok, failures={failures}")
    if failures:
        return 2

    map_path = os.path.join(MEAS, "phase2_header_fix.csv")
    with open(map_path, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["file", "upstream_ref", "kind", "body_sha256"])
        w.writeheader()
        w.writerows(out_rows)
    print(f"wrote {map_path}")

    if not args.apply:
        print("DRY-RUN: nothing written (pass --apply)")
        return 0

    # --- patch canonical_verdicts.csv (owner-ruled reclassification) ---
    cv_path = os.path.join(MEAS, "canonical_verdicts.csv")
    with open(cv_path, newline="") as fh:
        rows = list(csv.DictReader(fh))
        fields = list(rows[0].keys())
    patched = 0
    for rc in RECLASS:
        for r in rows:
            if r["file"] == rc["file"]:
                r["verdict"] = rc["verdict"]
                r["upstream_ref"] = rc["upstream_ref"]
                r["similarity_pct"] = rc["similarity_pct"]
                r["note"] = rc["note"]
                patched += 1
    with open(cv_path, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)
    print(f"canonical_verdicts.csv: reclassified {patched}/2 rows")

    # --- patch phase1_header_map.csv rows for the 5 ---
    hm_path = os.path.join(MEAS, "phase1_header_map.csv")
    with open(hm_path, newline="") as fh:
        rows = list(csv.DictReader(fh))
        fields = list(rows[0].keys())
    patched = 0
    for fx in FIXES:
        for r in rows:
            if r["file"] == fx["file"]:
                r["action"] = "phase2-restore-header"
                r["upstream"] = fx["ref"]
                r["kind"] = fx["kind"]
                patched += 1
    with open(hm_path, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)
    print(f"phase1_header_map.csv: updated {patched}/5 rows")
    return 0


if __name__ == "__main__":
    sys.exit(main())
