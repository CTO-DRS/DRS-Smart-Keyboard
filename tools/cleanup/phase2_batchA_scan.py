#!/usr/bin/env python3
"""Phase 2 Batch A — pre-kill symbol scan for stage-1 file candidates.

The detector's stage 1 does NOT run stage 2 on its own candidates. A file
whose symbols are referenced without ever typing the file stem (extension
functions, common helper names, class imports) would be a stage-1 false
positive. This scan closes that gap BEFORE any deletion:

  for each stage-1 candidate file:
      extract top-level declaration names (incl. extension property names)
      count whole-word corpus references EXCLUDING the file itself
      verdict: KILL (all symbols 0 refs) | KEEP (any symbol referenced)

Conservative: references found in comments/KDoc count (they credit the file).
Protected-4 files are always KEEP regardless.
Read-only. The compiler remains the final arbiter after deletion.
"""
from __future__ import annotations

import argparse
import csv
import os
import re
import sys
from pathlib import Path

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from deadcode_detector import DECL, EXT_PROP, tracked_sources  # noqa: E402

MEAS = os.path.join(HERE, "measurements")
PROTECTED_STEMS = {"Tooltip", "TooltipDefaults", "MotionScale", "KeyboardForwardDelete"}


MODS = (r"(?:public\s+|private\s+|internal\s+|protected\s+|open\s+|"
        r"abstract\s+|final\s+|sealed\s+|data\s+|value\s+|inline\s+|"
        r"external\s+|suspend\s+|operator\s+|const\s+|lateinit\s+|"
        r"expect\s+|actual\s+)*")
DECL_FUN = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s+)*" + MODS + r"fun\s+(?:<[^>]*>\s*)?"
    r"(?:[\w.`?<>]+\.)*([A-Za-z_][\w`]*)\s*[(<]")
DECL_VAL = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s+)*" + MODS + r"(?:val|var)\s+"
    r"(?:<[^>]*>\s*)?(?:[\w.`?<>]+\.)*([A-Za-z_][\w`]*)\s*[:<=(]")
DECL_TYPE = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s+)*" + MODS + r"(?:fun\s+)?"
    r"(?:annotation\s+)?(?:class|object|interface)\b\s*([`A-Za-z_][`A-Za-z0-9_]*)")


def top_level_symbols(text: str) -> list[str]:
    """Extract REAL declared names: for `fun Receiver.name(...)` and
    `val Receiver.name`, the captured name is the last segment (the actual
    member being declared), never the receiver type."""
    names: list[str] = []
    for line in text.splitlines():
        if not line or line[0] in " \t/}]" or line.startswith("*"):
            continue
        m = DECL_FUN.match(line) or DECL_VAL.match(line) or DECL_TYPE.match(line)
        if m:
            name = m.group(m.lastindex).strip("`")
            if name and name not in ("fun", "val", "var", "class", "object", "interface"):
                names.append(name)
    return names


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=os.path.abspath(os.path.join(HERE, "..", "..")))
    ap.add_argument("--candidates", default=os.path.join(MEAS, "deadcode_phase2_presolve.csv"))
    ap.add_argument("--out", default=os.path.join(MEAS, "phase2_batchA_scan.csv"))
    args = ap.parse_args()
    repo = Path(args.repo)

    with open(args.candidates, newline="") as fh:
        cands = [r for r in csv.DictReader(fh) if r["kind"] == "file"]

    corpus_files = tracked_sources(repo)
    corpus_texts: list[tuple[Path, str]] = []
    for p in corpus_files:
        try:
            corpus_texts.append((p, p.read_text(encoding="utf-8", errors="replace")))
        except OSError:
            pass

    rows = []
    for c in cands:
        rel = c["file"]
        f = repo / rel
        stem = Path(rel).stem
        text = f.read_text(encoding="utf-8", errors="replace")
        symbols = top_level_symbols(text)
        refs_by_symbol: dict[str, int] = {}
        for name in set(symbols):
            pat = re.compile(rf"\b{re.escape(name)}\b")
            n = 0
            for cp, ct in corpus_texts:
                if cp == f:
                    continue
                n += len(pat.findall(ct))
            refs_by_symbol[name] = n
        referenced = {k: v for k, v in refs_by_symbol.items() if v > 0}
        if stem in PROTECTED_STEMS:
            verdict = "KEEP-protected"
        elif referenced:
            verdict = "KEEP-symbols-referenced"
        else:
            verdict = "KILL"
        rows.append({
            "file": rel, "symbols": len(symbols),
            "referenced_symbols": ";".join(sorted(referenced)) or "-",
            "max_refs": max(refs_by_symbol.values()) if refs_by_symbol else 0,
            "verdict": verdict,
        })
        print(f"{verdict:24} {rel}" +
              (f"  <- {sorted(referenced)[:4]}" if referenced else ""))

    with open(args.out, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["file", "symbols", "referenced_symbols",
                                           "max_refs", "verdict"])
        w.writeheader()
        w.writerows(rows)
    kills = sum(1 for r in rows if r["verdict"] == "KILL")
    print(f"\nfile candidates: {len(rows)} | kill={kills} "
          f"keep={len(rows) - kills} -> {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
