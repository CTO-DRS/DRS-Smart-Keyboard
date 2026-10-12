#!/usr/bin/env python3
"""Unified reverse-similarity metric — DRS cleanup protocol (canonical tool).

Measures how much of OUR lib/ file still matches its upstream origin, after
normalization. Target: <= 30% for every rewritten file (protocol rule), with
documented exemptions only.

Normalization — identical for both sides (protocol rule 10):
  1. Strip UTF-8 BOM.
  2. Remove the license header (leading block comment at file start).
  3. Remove comments (// and /* */) with a string-aware scanner.
  4. Drop blank lines; trim whitespace on every remaining line.
  5. Neutralize package/import lines (packages were renamed org.drs.* vs the
     upstream namespaces): each such line collapses to its keyword, so a pure
     package-rename copy still scores ~100%.
  6. Drop "bare brace" lines (only {}()[];, characters) from BOTH sides
     equally.

Metric: multiset line intersection / our normalized line count * 100.
  matched = sum(min(count_ours[line], count_upstream[line]))
  pct     = matched * 100 / count_ours
Interpretation: "x% of our file's normalized lines are still verbatim
upstream lines". 0% = fully original, 100% = verbatim copy.

Usage:
  Single:  measure_similarity.py --file <lib-file> --upstream <upstream-file>
  Batch:   measure_similarity.py --batch mapping.csv --out results.csv
           (mapping.csv columns: file,upstream)
  Canonical table upsert (builds incrementally, committed per phase):
           measure_similarity.py --file F --upstream U --phase 3 \
               --stage before|after --canonical measurements/canonical_similarity.csv

Exit code 0 always (measurement tool); use --fail-above to enforce a gate:
  --fail-above 30  -> exit 1 if any measured file exceeds 30%.
"""
from __future__ import annotations

import argparse
import collections
import csv
import datetime as _dt
import hashlib
import re
import sys
from pathlib import Path

TOOL_VERSION = "1.0.0"

_BARE_LINE = re.compile(r"^[\{\}\(\)\[\];,]*$")
_PKG = re.compile(r"^\s*package\s[\w.]*")
_IMP = re.compile(r"^\s*import\s[\w.*]*")


def _strip_comments(text: str) -> str:
    """String-aware comment removal for Kotlin-ish sources."""
    out: list[str] = []
    i, n = 0, len(text)
    state = "code"  # code | line | block | str | char | triple
    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if state == "code":
            if c == "/" and nxt == "/":
                state = "line"
                i += 2
                continue
            if c == "/" and nxt == "*":
                state = "block"
                i += 2
                continue
            if text.startswith('"""', i):
                state = "triple"
                out.append('"""')
                i += 3
                continue
            if c == '"':
                state = "str"
                out.append(c)
                i += 1
                continue
            if c == "'":
                state = "char"
                out.append(c)
                i += 1
                continue
            out.append(c)
            i += 1
        elif state == "line":
            if c == "\n":
                state = "code"
                out.append("\n")
            i += 1
        elif state == "block":
            if c == "*" and nxt == "/":
                state = "code"
                i += 2
            else:
                if c == "\n":
                    out.append("\n")
                i += 1
        elif state == "str":
            if c == "\\":
                out.append(text[i:i + 2])
                i += 2
                continue
            if c == '"':
                state = "code"
            out.append(c)
            i += 1
        elif state == "char":
            if c == "\\":
                out.append(text[i:i + 2])
                i += 2
                continue
            if c == "'":
                state = "code"
            out.append(c)
            i += 1
        elif state == "triple":
            if text.startswith('"""', i):
                state = "code"
                out.append('"""')
                i += 3
                continue
            out.append(c)
            i += 1
    return "".join(out)


def _strip_header(text: str) -> str:
    """Remove a leading license header: blank lines then one /* ... */ block."""
    s = text.lstrip("\ufeff \t\r\n")
    if not s.startswith("/*"):
        return text
    end = s.find("*/")
    if end == -1:
        return text
    return s[end + 2:].lstrip("\r\n")


def normalize_lines(raw: str) -> list[str]:
    text = _strip_header(raw)
    text = _strip_comments(text)
    lines: list[str] = []
    for ln in text.splitlines():
        ln = ln.strip()
        if not ln:
            continue
        if _PKG.match(ln):
            ln = "package"
        elif _IMP.match(ln):
            ln = "import"
        if _BARE_LINE.match(ln):
            continue
        lines.append(ln)
    return lines


def measure(ours_raw: str, upstream_raw: str) -> dict:
    ours = normalize_lines(ours_raw)
    up = normalize_lines(upstream_raw)
    c_ours = collections.Counter(ours)
    c_up = collections.Counter(up)
    matched = sum(min(v, c_up[k]) for k, v in c_ours.items())
    pct = (matched * 100.0 / len(ours)) if ours else 0.0
    return {
        "ours_lines": len(ours),
        "upstream_lines": len(up),
        "matched_lines": matched,
        "similarity_pct": round(pct, 1),
    }


def _digest(raw: str) -> str:
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()[:16]


def run_single(args: argparse.Namespace) -> int:
    ours_p, up_p = Path(args.file), Path(args.upstream)
    ours_raw = ours_p.read_text(encoding="utf-8", errors="replace")
    up_raw = up_p.read_text(encoding="utf-8", errors="replace")
    res = measure(ours_raw, up_raw)
    row = {
        "file": str(ours_p), "upstream": str(up_p), **res,
        "ours_sha": _digest(ours_raw), "upstream_sha": _digest(up_raw),
    }
    if args.phase is not None and args.canonical:
        upsert_canonical(args.canonical, row, args.phase, args.stage)
    if args.out:
        append_csv(args.out, row)
    print(",".join(["file", "upstream", "ours_lines", "upstream_lines",
                    "matched_lines", "similarity_pct"]))
    print(",".join([row["file"], row["upstream"], str(row["ours_lines"]),
                    str(row["upstream_lines"]), str(row["matched_lines"]),
                    str(row["similarity_pct"])]))
    if args.fail_above is not None and res["similarity_pct"] > args.fail_above:
        print(f"GATE FAIL: {row['file']} = {res['similarity_pct']}% "
              f"> {args.fail_above}%", file=sys.stderr)
        return 1
    return 0


def run_batch(args: argparse.Namespace) -> int:
    fail = False
    with open(args.batch, newline="", encoding="utf-8") as fh:
        rows = list(csv.DictReader(fh))
    results = []
    for r in rows:
        ours_raw = Path(r["file"]).read_text(encoding="utf-8", errors="replace")
        up_raw = Path(r["upstream"]).read_text(encoding="utf-8", errors="replace")
        res = measure(ours_raw, up_raw)
        results.append({"file": r["file"], "upstream": r["upstream"], **res})
        if args.fail_above is not None and res["similarity_pct"] > args.fail_above:
            fail = True
    if args.out:
        with open(args.out, "w", newline="", encoding="utf-8") as fh:
            w = csv.DictWriter(fh, fieldnames=["file", "upstream", "ours_lines",
                                               "upstream_lines", "matched_lines",
                                               "similarity_pct"])
            w.writeheader()
            w.writerows(results)
    for r in results:
        print(f"{r['similarity_pct']:>6.1f}%  {r['file']}")
    return 1 if fail else 0


def upsert_canonical(canonical: str, row: dict, phase: int, stage: str) -> None:
    cpath = Path(canonical)
    cpath.parent.mkdir(parents=True, exist_ok=True)
    rows: dict[str, dict] = {}
    fieldnames = ["file", "upstream", "phase", "before_pct", "after_pct",
                  "ours_lines", "matched_lines", "last_measured", "tool_version"]
    if cpath.exists():
        with cpath.open(newline="", encoding="utf-8") as fh:
            for r in csv.DictReader(fh):
                rows[r["file"]] = r
    rec = rows.setdefault(row["file"], {"file": row["file"]})
    rec.update({
        "upstream": row["upstream"],
        "phase": str(phase),
        f"{stage}_pct": str(row["similarity_pct"]),
        "ours_lines": str(row["ours_lines"]),
        "matched_lines": str(row["matched_lines"]),
        "last_measured": _dt.datetime.now(_dt.timezone.utc).isoformat(timespec="seconds"),
        "tool_version": TOOL_VERSION,
    })
    with cpath.open("w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=fieldnames)
        w.writeheader()
        for k in sorted(rows):
            w.writerow(rows[k])


def append_csv(out: str, row: dict) -> None:
    p = Path(out)
    p.parent.mkdir(parents=True, exist_ok=True)
    exists = p.exists()
    fields = ["file", "upstream", "ours_lines", "upstream_lines",
              "matched_lines", "similarity_pct", "ours_sha", "upstream_sha"]
    with p.open("a", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=fields)
        if not exists:
            w.writeheader()
        w.writerow(row)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--file")
    ap.add_argument("--upstream")
    ap.add_argument("--batch")
    ap.add_argument("--out")
    ap.add_argument("--phase", type=int)
    ap.add_argument("--stage", choices=["before", "after"])
    ap.add_argument("--canonical")
    ap.add_argument("--fail-above", type=float)
    args = ap.parse_args()
    if args.batch:
        return run_batch(args)
    if args.file and args.upstream:
        return run_single(args)
    ap.print_help()
    return 2


if __name__ == "__main__":
    sys.exit(main())
