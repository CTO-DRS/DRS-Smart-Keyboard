#!/usr/bin/env python3
"""Phase 2 Batch C — orphaned private members.

Owner category (c): private members whose final verdict is the official
compiler. Empirical finding (documented in WORKLOG): this toolchain
(Kotlin 2.4.20 via AGP9 Build Tools API) emits NO 'never used' diagnostics
(probe-verified with 4 deliberately-dead private declarations, batchC probe
compile green, zero warnings). The compiler-as-final-arbiter principle is
therefore applied directly: detector private-member candidates + trial
deletion + full build/test gate.

Scope rules:
  - 'private' members everywhere; '+ internal' inside lib/jetpref only
    (owner: dead kills inside jetpref allowed, public API frozen).
  - candidate must have ZERO occurrences project-wide outside its own
    declaration line (self-inclusive text scan).
  - protected-4 files untouched.
Journal allows exact per-file rollback on compiler failure.
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

MEM_PAT = re.compile(
    r"^(\s*)(?:@\w+(?:\([^)]*\))?\s+)*"
    r"(?:private\s+|internal\s+|public\s+|open\s+|final\s+|inline\s+|"
    r"suspend\s+|const\s+|lateinit\s+|annotation\s+|operator\s+)*"
    r"private\s+(?:const\s+|lateinit\s+|inline\s+|suspend\s+|annotation\s+|open\s+|"
    r"(?:fun|val|var|class|object|interface)\b)")
# also capture the declared name (last identifier before ( < : = or end)
NAME_PAT = re.compile(
    r"(?:fun|val|var|class|object|interface)\s+(?:<[^>]*>\s*)?"
    r"(?:[\w.`?<>]+\.)*([A-Za-z_][\w`]*)")
ATTACHED_ABOVE = re.compile(r"^\s*(/\*\*|\*/|\*|//|@)")
OPEN_BRACES = "{(["


def find_member_line(lines: list[str], name: str) -> int:
    for i, ln in enumerate(lines):
        if MEM_PAT.match(ln):
            m = NAME_PAT.search(ln)
            if m and m.group(1).strip("`") == name:
                return i
    return -1


def block_end(lines: list[str], start: int) -> int:
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
            s = lines[i].rstrip()
            if i > start and not s.endswith((",", "(", "=", ".", "?", ":", ";", "{")):
                return i
    return len(lines) - 1


def attached_start(lines: list[str], decl: int) -> int:
    s = decl
    while s - 1 >= 0:
        prev = lines[s - 1]
        if not prev.strip():
            break
        if ATTACHED_ABOVE.match(prev):
            s -= 1
        else:
            break
    return s


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(HERE, "..", "..")))
    ap.add_argument("--journal", default=os.path.join(MEAS, "phase2_batchC_journal.json"))
    ap.add_argument("--out", default=os.path.join(MEAS, "phase2_batchC_removed.csv"))
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args()
    repo = Path(args.repo)

    with open(os.path.join(MEAS, "phase2_batchB_skipped.csv"), newline="") as fh:
        pool = [r for r in csv.DictReader(fh) if r["why"] == "decl-not-found"]

    # corpus for project-wide self-inclusive scan
    corpus: list[tuple[Path, str]] = []
    git_files = os.popen("git -C . ls-files").read().splitlines()
    for gf in git_files:
        if gf.endswith((".kt", ".kts", ".xml")):
            p = Path(gf)
            try:
                corpus.append((p, p.read_text(encoding="utf-8", errors="replace")))
            except OSError:
                pass

    journal: dict[str, str] = {}
    removed: list[dict] = []
    skipped: list[dict] = []

    for c in pool:
        rel, kind, name = c["file"], c["kind"], c["name"]
        if Path(rel).stem in PROTECTED_STEMS or not name:
            continue
        f = repo / rel
        if not f.exists():
            continue
        text = f.read_text(encoding="utf-8")
        lines = text.splitlines()
        decl = find_member_line(lines, name)
        if decl < 0:
            skipped.append({"file": rel, "kind": kind, "name": name, "why": "decl-not-found"})
            continue
        decl_line = lines[decl]
        is_jetpref = rel.startswith("lib/jetpref/")
        if is_jetpref:
            if "private" not in decl_line and "internal" not in decl_line:
                skipped.append({"file": rel, "kind": kind, "name": name,
                                "why": "jetpref-public-api-frozen"})
                continue
        else:
            if "private" not in decl_line:
                skipped.append({"file": rel, "kind": kind, "name": name,
                                "why": "not-private(non-jetpref)"})
                continue
        # self-inclusive project-wide scan: any occurrence outside own decl line
        pat = re.compile(rf"\b{re.escape(name)}\b")
        refs = 0
        for cp, ct in corpus:
            for m in pat.finditer(ct):
                # exclude the declaration line itself
                line_no = ct.count("\n", 0, m.start()) + 1
                if cp == f and line_no == decl + 1:
                    continue
                refs += 1
                if refs:
                    break
            if refs:
                break
        if refs:
            skipped.append({"file": rel, "kind": kind, "name": name, "why": "referenced"})
            continue
        start = attached_start(lines, decl)
        end = block_end(lines, decl)
        new_text = "".join(lines[:start]) + "".join(lines[end + 1:])
        if rel not in journal:
            journal[rel] = text
        if args.apply:
            f.write_text(new_text, encoding="utf-8")
        removed.append({"file": rel, "kind": kind, "name": name, "lines": end - start + 1})
        print(f"removed {kind:9} {name:36} {rel.split('/')[-1]} ({end - start + 1} lines)")

    print(f"\napply={args.apply} removed={len(removed)} skipped={len(skipped)}")
    with open(args.out, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["file", "kind", "name", "lines"])
        w.writeheader()
        w.writerows(removed)
    with open(os.path.join(MEAS, "phase2_batchC_skipped.csv"), "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["file", "kind", "name", "why"])
        w.writeheader()
        w.writerows(skipped)
    if args.apply:
        with open(args.journal, "w") as fh:
            json.dump(journal, fh)
    return 0


if __name__ == "__main__":
    sys.exit(main())
