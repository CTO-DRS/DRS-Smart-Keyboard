#!/usr/bin/env python3
"""Phase 2 Batch B — kill dead top-level symbols (owner-approved categories).

Rules baked in:
  - Protected-4 files never touched.
  - jetpref modules (lib/jetpref/**): ONLY private/internal symbols are
    killable — public API surface is frozen (owner rule: dead kill only,
    no API modification).
  - ext-prop candidates: always SKIPPED (T1 extension-property trap).
  - The compiler is the arbiter: this tool never compiles; the driver
    compiles, and on failure restores whole files from the journal.

Removal block = attached preceding KDoc/annotations + declaration through
balanced-brace/expression end. Journal stores each file's original text so
restore is exact.
"""
from __future__ import annotations

import argparse
import csv
import json
import os
import re
import sys
from pathlib import Path

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from phase2_batchA_scan import DECL_FUN, DECL_VAL, DECL_TYPE  # noqa: E402
MEAS = os.path.join(HERE, "measurements")
PROTECTED_STEMS = {"Tooltip", "TooltipDefaults", "MotionScale", "KeyboardForwardDelete"}

KIND_PAT = {
    "top-fun": DECL_FUN,
    "top-val": DECL_VAL,
    "top-var": DECL_VAL,
    "top-class": DECL_TYPE,
    "top-object": DECL_TYPE,
    "top-interface": DECL_TYPE,
}
ATTACHED_ABOVE = re.compile(r"^\s*(/\*\*|\*/|\*|//|@|expect\s*$)")
OPEN_BRACES = "{(["


def find_decl_line(lines: list[str], kind: str, name: str) -> int:
    """Column-0 anchored: the DECL_* regexes from the batch-A scanner match
    top-level declarations only (indented members never match)."""
    pat = KIND_PAT.get(kind)
    if pat is None:
        return -1
    for i, ln in enumerate(lines):
        if ln and ln[0] not in " \t" and pat.match(ln):
            m = pat.match(ln)
            if m and m.group(m.lastindex).strip("`") == name:
                return i
    return -1


def block_end(lines: list[str], start: int) -> int:
    """Line index where the declaration ends (brace-balanced or expression)."""
    depth = 0
    opened = False
    for i in range(start, len(lines)):
        for ch in lines[i]:
            if ch in OPEN_BRACES:
                depth += 1
                opened = True
            elif ch in ")]}":
                depth -= 1
        if opened and depth <= 0:
            return i
        if not opened:
            # single-expression decl: end when line doesn't continue
            s = lines[i].rstrip()
            if i > start and not s.endswith((",", "(", "=", ".", "?", ":", ";")) and not s.endswith("=>"):
                return i
    return len(lines) - 1


def attached_start(lines: list[str], decl: int) -> int:
    s = decl
    while s - 1 >= 0:
        prev = lines[s - 1]
        if not prev.strip():
            break
        if ATTACHED_ABOVE.match(prev) or prev.rstrip().endswith(")") and prev.lstrip().startswith("@"):
            s -= 1
        else:
            break
    # include the whole KDoc block if we touched one
    if s - 1 >= 0 and lines[s - 1].lstrip().startswith("*/") and s - 1 >= 1:
        pass  # already included via /* */ lines
    return s


def remove_block(text: str, kind: str, name: str) -> tuple[str, int, int] | None:
    lines = text.splitlines(keepends=True)
    decl = find_decl_line(lines, kind, name)
    if decl < 0:
        return None
    start = attached_start(lines, decl)
    end = block_end(lines, decl)
    new_text = "".join(lines[:start]) + "".join(lines[end + 1:])
    return new_text, start, end


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(HERE, "..", "..")))
    ap.add_argument("--candidates", default=os.path.join(MEAS, "deadcode_phase2_batchB_input.csv"))
    ap.add_argument("--journal", default=os.path.join(MEAS, "phase2_batchB_journal.json"))
    ap.add_argument("--out", default=os.path.join(MEAS, "phase2_batchB_removed.csv"))
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()
    repo = Path(args.repo)

    with open(args.candidates, newline="") as fh:
        cands = [r for r in csv.DictReader(fh) if r["kind"] not in ("file", "ext-prop", "kind")]

    journal: dict[str, str] = {}
    removed: list[dict] = []
    skipped: list[dict] = []

    for c in cands:
        rel, kind, name = c["file"], c["kind"], c["name"]
        if Path(rel).stem in PROTECTED_STEMS:
            skipped.append({"file": rel, "kind": kind, "name": name, "why": "protected"})
            continue
        f = repo / rel
        text = f.read_text(encoding="utf-8")
        is_jetpref = rel.startswith("lib/jetpref/")
        # locate decl line to enforce jetpref visibility rule
        lines = text.splitlines()
        decl = find_decl_line(lines, kind, name)
        if decl < 0:
            skipped.append({"file": rel, "kind": kind, "name": name, "why": "decl-not-found"})
            continue
        # self-inclusive usage filter: the detector's 0-ref count EXCLUDES the
        # file itself, so intra-file callers are invisible to it. Any other
        # occurrence of the name inside its own file => KEEP this round.
        others = [ln for i, ln in enumerate(lines) if i != decl and name in ln]
        if others:
            skipped.append({"file": rel, "kind": kind, "name": name, "why": "intra-file-usage"})
            continue
        if is_jetpref:
            head = lines[decl]
            if "private" not in head and "internal" not in head:
                skipped.append({"file": rel, "kind": kind, "name": name, "why": "jetpref-public-api-frozen"})
                continue
        res = remove_block(text, kind, name)
        if res is None:
            skipped.append({"file": rel, "kind": kind, "name": name, "why": "block-not-parsed"})
            continue
        new_text, start, end = res
        if rel not in journal:
            journal[rel] = text
        if args.apply:
            f.write_text(new_text, encoding="utf-8")
        removed.append({"file": rel, "kind": kind, "name": name,
                        "lines": end - start + 1})
        print(f"removed {kind:12} {name:38} {rel} ({end - start + 1} lines)")

    print(f"\napply={args.apply} removed={len(removed)} skipped={len(skipped)} "
          f"files_touched={len(journal)}")
    with open(args.out, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["file", "kind", "name", "lines"])
        w.writeheader()
        w.writerows(removed)
    with open(os.path.join(MEAS, "phase2_batchB_skipped.csv"), "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["file", "kind", "name", "why"])
        w.writeheader()
        w.writerows(skipped)
    if args.apply:
        with open(args.journal, "w") as fh:
            json.dump(journal, fh)
        print(f"journal: {args.journal}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
