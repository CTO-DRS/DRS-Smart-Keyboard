#!/usr/bin/env python3
"""Restore original Apache-2.0 license headers over vendored lib/ files.

DRS cleanup protocol — Phase 1 tool (headers only, ZERO code change).

Behavior:
  1. Index upstream files from the PINNED measurement refs (floris main
     @ fe1241f, jetpref main @ 7a3df4e — pins are final): verbatim license
     header + normalized-body fingerprint per file. Per-repo canonical
     header = the most frequent header in that repo (used when a matched
     upstream file itself has no header).
  2. For every lib/ file: skip files already carrying an Apache-2.0 header
     (AOSP-protected quartet etc.) or listed in --exclude. Otherwise strip
     the DRS placeholder header (4-line block, if present), match the body
     against upstream by exact fingerprint, then by filename (including the
     vendoring rename Drs* <- Floris*), else fuzzy by name (difflib).
  3. Write: upstream header (verbatim) — or canonical repo header when the
     upstream file has none — plus the UNCHANGED body. Body SHA-256 before
     == after is asserted for every written file.
  4. Emit a full mapping report (every file, action, upstream, kind) for
     the phase gate.

Dry-run is the DEFAULT. Pass --apply to write anything.
"""
from __future__ import annotations

import argparse
import collections
import csv
import difflib
import hashlib
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from measure_similarity import normalize_lines  # noqa: E402

PROTECTED_NAMES = {
    "Tooltip.kt", "TooltipDefaults.kt", "MotionScale.kt", "KeyboardForwardDelete.kt",
}


def extract_header(raw: str) -> str:
    s = raw.lstrip("\ufeff \t\r\n")
    if s.startswith("/*"):
        end = s.find("*/")
        if end != -1:
            return s[:end + 2]
    return ""


def body_after_header(raw: str) -> str:
    s = raw.lstrip("\ufeff \t\r\n")
    if s.startswith("/*"):
        end = s.find("*/")
        if end != -1:
            return s[end + 2:].lstrip("\r\n")
    return raw


def candidate_names(fname: str) -> list[str]:
    names = [fname]
    stem = Path(fname).stem
    if stem.startswith("Drs"):
        names.append(f"Floris{stem[3:]}.kt")
    return names


def iter_sources(root: Path, suffixes: tuple[str, ...]):
    for p in root.rglob("*"):
        if p.is_file() and p.suffix in suffixes:
            if any(part in ("build", ".git", ".gradle") for part in p.parts):
                continue
            yield p


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--floris", required=True)
    ap.add_argument("--jetpref", required=True)
    ap.add_argument("--modules", default="android,color,compose,kotlin,snygg,jetpref")
    ap.add_argument("--min-ratio", type=float, default=0.75)
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--report")
    args = ap.parse_args()

    repo = Path(args.repo)
    suffixes = (".kt", ".kts")
    repos = (("floris", Path(args.floris)), ("jetpref", Path(args.jetpref)))

    by_fp: dict[str, list[tuple[str, str]]] = {}
    by_name: dict[str, list[tuple[str, str, list[str], str]]] = {}
    header_mode: dict[str, str] = {}
    header_votes: dict[str, collections.Counter] = {t: collections.Counter() for t, _ in repos}
    for tag, root in repos:
        for p in iter_sources(root, suffixes):
            raw = p.read_text(encoding="utf-8", errors="replace")
            header = extract_header(raw)
            body = body_after_header(raw)
            lines = normalize_lines(body)
            fp = hashlib.sha256("\n".join(lines).encode()).hexdigest()
            rel = str(p.relative_to(root))
            by_fp.setdefault(fp, []).append((header, f"{tag}:{rel}"))
            by_name.setdefault(p.name, []).append((header, f"{tag}:{rel}", lines, tag))
            if header:
                header_votes[tag][header] += 1
    for tag, _ in repos:
        if header_votes[tag]:
            header_mode[tag] = header_votes[tag].most_common(1)[0][0]
    print(f"upstream index: by_fp={len(by_fp)} bodies; canonical headers: "
          f"floris={'yes' if 'floris' in header_mode else 'NO'}, "
          f"jetpref={'yes' if 'jetpref' in header_mode else 'NO'}")

    lib_root = repo / "lib"
    modules = [m.strip() for m in args.modules.split(",") if m.strip()]
    stats: collections.Counter = collections.Counter()
    report: list[dict] = []

    for module in modules:
        mroot = lib_root / module
        if not mroot.exists():
            continue
        for f in sorted(iter_sources(mroot, suffixes)):
            rel = str(f.relative_to(repo))
            raw = f.read_text(encoding="utf-8", errors="replace")

            def row(action: str, upstream: str = "-", kind: str = "") -> None:
                report.append({"file": rel, "action": action,
                               "upstream": upstream, "kind": kind})
                stats[action] += 1

            if f.name in PROTECTED_NAMES:
                row("protected-skip")
                continue
            current_header = extract_header(raw)
            if "Apache License" in current_header:
                row("already-apache")
                continue
            body = body_after_header(raw) if current_header else raw
            fp = hashlib.sha256("\n".join(normalize_lines(body)).encode()).hexdigest()
            hit_header, hit_ref, hit_tag, kind = "", "", "", ""
            exact = by_fp.get(fp)
            if exact:
                hit_header, hit_ref = exact[0]
                hit_tag = hit_ref.split(":", 1)[0]
                kind = "exact"
            else:
                best, best_r = None, 0.0
                our_lines = normalize_lines(body)
                for name in candidate_names(f.name):
                    for (header, upref, ulines, tag) in by_name.get(name, []):
                        if not header and tag not in header_mode:
                            continue
                        r = difflib.SequenceMatcher(a=our_lines, b=ulines,
                                                    autojunk=False).ratio()
                        if r > best_r:
                            best, best_r = (header, upref, tag), r
                if best and best_r >= args.min_ratio:
                    hit_header, hit_ref, hit_tag = best
                    kind = f"fuzzy:{best_r:.2f}"
            if not hit_ref:
                row("no-match-keep-drs-header")
                continue
            if not hit_header:
                hit_header = header_mode.get(hit_tag, "")
                if not hit_header:
                    row("no-upstream-header", hit_ref, kind)
                    continue
                kind += "+canonical-header"
            new_text = hit_header + "\n\n" + body
            sha_before = hashlib.sha256(body.encode()).hexdigest()
            sha_after = hashlib.sha256(
                new_text[len(hit_header) + 2:].encode()).hexdigest()
            if sha_before != sha_after:
                print(f"FATAL: body changed for {f}", file=sys.stderr)
                return 1
            row("restore", hit_ref, kind)
            if args.apply:
                f.write_text(new_text, encoding="utf-8", newline="")

    for r in report:
        print(f"{r['action']:<24} {r['kind']:<18} {r['file']}")
    print("STATS: " + ", ".join(f"{k}={v}" for k, v in sorted(stats.items())))
    if args.report:
        rp = Path(args.report)
        rp.parent.mkdir(parents=True, exist_ok=True)
        with rp.open("w", newline="", encoding="utf-8") as fh:
            w = csv.DictWriter(fh, fieldnames=["file", "action", "upstream", "kind"])
            w.writeheader()
            w.writerows(report)
        print(f"report: {rp}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
