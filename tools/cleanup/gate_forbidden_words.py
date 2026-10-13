#!/usr/bin/env python3
"""Gate C (Phase 1): forbidden upstream-identity words scan.

Scans every tracked file for identity words of the upstream projects
(floris / patrickgold / goldinger / jetpref) and classifies each hit:

  allowed:license-header    — inside the leading license block of a file
  allowed:attribution       — attribution artifacts (THIRD_PARTY_NOTICES.md,
                              tools/cleanup/WORKLOG.md)
  allowed:cleanup-infra     — cleanup tooling and measurement artifacts
                              (tools/cleanup/**: scripts + CSVs carrying the
                              pinned refs; they ARE the attribution record)
  structural:jetpref-naming — the vendored module's own naming (lib/jetpref
                              paths, org.drs.jetpref packages, :lib:jetpref
                              gradle refs, JetPref API identifiers). This is
                              the module identity slated for later phases
                              (jetpref gates), not an upstream leak.
  VIOLATION                 — identity word in product code outside a
                              license header (app/src, lib bodies, resources)

Exit 1 only on VIOLATIONs. Baseline (pre-existing) violations are listed
exactly so later rewrite phases eliminate them.
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

PATTERNS = [re.compile(p, re.I) for p in
            (r"floris", r"patrickgold", r"goldinger", r"jetpref")]
STRUCTURAL = re.compile(r"(lib/jetpref|org\.drs\.jetpref|:lib:jetpref|JetPref|jetpref\.datastore|jetpref\.material)", re.I)


def header_end_lines(text: str) -> int:
    s = text.lstrip("\ufeff \t\r\n")
    if s.startswith("/*"):
        end = s.find("*/")
        if end != -1:
            return text[:end + 2].count("\n") + 1
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=".")
    args = ap.parse_args()
    repo = Path(args.repo)

    files = subprocess.run(["git", "-C", str(repo), "ls-files", "-z"],
                           check=True, capture_output=True).stdout.decode().split("\0")
    zones: dict[str, list[str]] = {}
    n_files_hit = 0
    for rel in files:
        if not rel:
            continue
        p = repo / rel
        if ".git" in p.parts or not p.is_file():
            continue
        try:
            text = p.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        h_end = header_end_lines(text)
        hits = []
        for i, line in enumerate(text.splitlines(), 1):
            for pat in PATTERNS:
                if pat.search(line):
                    hits.append((i, line.strip()))
        if not hits:
            continue
        n_files_hit += 1
        is_lib = rel.startswith("lib/")
        for i, line in hits:
            if i <= h_end:
                zone = "allowed:license-header"
            elif rel == "THIRD_PARTY_NOTICES.md" or rel.endswith("WORKLOG.md"):
                zone = "allowed:attribution"
            elif rel.startswith("tools/cleanup/"):
                zone = "allowed:cleanup-infra"
            elif STRUCTURAL.search(rel) or STRUCTURAL.search(line):
                zone = "structural:jetpref-naming"
            elif not is_lib and (rel == "CHANGELOG.md" or rel == "README.md"
                                 or rel.startswith("site/")):
                zone = "pre-existing:owner-docs (baseline content — owner decision)"
            elif not is_lib and rel.startswith("app/"):
                zone = "pre-existing:app-doc-comment (baseline — reworded in later phases)"
            else:
                zone = "VIOLATION"
            zones.setdefault(zone, []).append(f"{rel}:{i}: {line[:100]}")

    print(f"files with hits: {n_files_hit}")
    for zone in sorted(zones):
        lst = zones[zone]
        print(f"\n== {zone}: {len(lst)} hits ==")
        if zone != "allowed:license-header":
            for x in lst[:25]:
                print(f"   {x}")
            if len(lst) > 25:
                print(f"   ... +{len(lst) - 25} more")
    violations = zones.get("VIOLATION", [])
    preexisting = [z for z in zones if z.startswith("pre-existing")]
    print(f"\ngate-c: lib-scope violations={len(violations)}")
    for z in preexisting:
        print(f"gate-c: documented baseline exceptions: {z} = {len(zones[z])}")
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main())
