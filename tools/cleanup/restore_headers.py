#!/usr/bin/env python3
"""Restore original Apache-2.0 license headers over vendored lib/ files.

DRS cleanup protocol — Phase 1 tool.

How it works:
  1. Build an index of upstream files (FlorisBoard + jetpref clones pinned to
     exact tags): for each *.kt / *.kts, extract its leading license header
     VERBATIM and fingerprint the normalized body (same normalization as
     measure_similarity.py, so package renames do not break matching).
  2. For every lib/ file in our repo: try exact normalized-body fingerprint
     match first; otherwise fuzzy-match against upstream files sharing the
     same filename (difflib ratio, threshold --min-ratio).
  3. Write: upstream header + blank line + ORIGINAL body bytes.
     Guarantee: body SHA-256 before == after (zero code change in Phase 1).
     Files that already start with a license header are skipped (idempotent).
  4. Optionally (re)build THIRD_PARTY_NOTICES.md with pinned upstream refs.

Dry-run is the DEFAULT. Pass --apply to write anything.

Usage:
  restore_headers.py --repo <repo-root> \
      --floris <floris-v0.4.6-clone> --jetpref <jetpref-v0.3.1-clone> \
      [--modules android,color] [--min-ratio 0.75] [--apply] \
      [--notices <THIRD_PARTY_NOTICES.md>] \
      [--pin-floris <sha>] [--pin-jetpref <sha>]
"""
from __future__ import annotations

import argparse
import difflib
import hashlib
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from measure_similarity import normalize_lines  # noqa: E402

FLORIS_SUBSTR = "florisboard"
JETPREF_SUBSTR = "jetpref"


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


def iter_sources(root: Path, suffixes: tuple[str, ...]):
    for p in root.rglob("*"):
        if p.is_file() and p.suffix in suffixes:
            if any(part in ("build", ".git", ".gradle") for part in p.parts):
                continue
            yield p


def build_upstream_index(paths: list[Path]) -> tuple[dict, dict]:
    by_fp: dict[str, list[tuple[Path, str]]] = {}
    by_name: dict[str, list[tuple[Path, str, list[str]]]] = {}
    for p in paths:
        raw = p.read_text(encoding="utf-8", errors="replace")
        header = extract_header(raw)
        body = body_after_header(raw)
        fp = hashlib.sha256("\n".join(normalize_lines(body)).encode()).hexdigest()
        by_fp.setdefault(fp, []).append((p, header))
        by_name.setdefault(p.name, []).append((p, header, normalize_lines(body)))
    return by_fp, by_name


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--floris", required=True)
    ap.add_argument("--jetpref", required=True)
    ap.add_argument("--modules", default="android,color,compose,kotlin,snygg,jetpref")
    ap.add_argument("--min-ratio", type=float, default=0.75)
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--notices")
    ap.add_argument("--pin-floris", default="<floris-pin>")
    ap.add_argument("--pin-jetpref", default="<jetpref-pin>")
    args = ap.parse_args()

    repo = Path(args.repo)
    suffixes = (".kt", ".kts")
    up_paths = list(iter_sources(Path(args.floris), suffixes)) + \
               list(iter_sources(Path(args.jetpref), suffixes))
    by_fp, by_name = build_upstream_index(up_paths)
    print(f"upstream index: {len(up_paths)} files "
          f"({len(by_fp)} distinct normalized bodies)")

    lib_root = repo / "lib"
    modules = [m.strip() for m in args.modules.split(",") if m.strip()]
    stats = {"restored": 0, "already": 0, "fuzzy": 0, "nomatch": 0, "noheader": 0}
    report: list[str] = []

    for module in modules:
        mroot = lib_root / module
        if not mroot.exists():
            continue
        for f in sorted(iter_sources(mroot, suffixes)):
            rel = f.relative_to(repo)
            raw = f.read_text(encoding="utf-8", errors="replace")
            current_header = extract_header(raw)
            if "Apache License" in current_header:
                stats["already"] += 1
                continue
            # DRS-placeholder header (or none): strip it, restore upstream one.
            # Body guarantee applies to the code AFTER the header block.
            body = body_after_header(raw) if current_header else raw
            fp = hashlib.sha256("\n".join(normalize_lines(body)).encode()).hexdigest()
            hit = by_fp.get(fp)
            kind = "exact"
            if not hit:
                cands = by_name.get(f.name, [])
                best, best_r = None, 0.0
                our_lines = normalize_lines(body)
                for (up, header, up_lines) in cands:
                    if not header:
                        continue
                    r = difflib.SequenceMatcher(
                        a=our_lines, b=up_lines, autojunk=False).ratio()
                    if r > best_r:
                        best, best_r = (up, header), r
                if best and best_r >= args.min_ratio:
                    hit, kind = [best], f"fuzzy:{best_r:.2f}"
            if not hit:
                stats["nomatch"] += 1
                report.append(f"{rel} -> NO MATCH")
                continue
            up_path, header = hit[0]
            if not header:
                stats["noheader"] += 1
                report.append(f"{rel} -> {up_path} (upstream has no header)")
                continue
            new_text = header + "\n\n" + body
            body_sha_before = hashlib.sha256(body.encode()).hexdigest()
            body_sha_after = hashlib.sha256(
                new_text[len(header) + 2:].encode()).hexdigest()
            assert body_sha_before == body_sha_after, f"BODY CHANGED: {f}"
            stats["restored" if kind == "exact" else "fuzzy"] += 1
            report.append(f"{rel} -> {up_path} [{kind}]")
            if args.apply:
                f.write_text(new_text, encoding="utf-8", newline="")

    for line in report:
        print(line)
    print("STATS: " + ", ".join(f"{k}={v}" for k, v in stats.items()))

    if args.apply and args.notices:
        notices = Path(args.notices)
        content = notices.read_text(encoding="utf-8") if notices.exists() else ""
        section = (
            "## Vendored / Derived Source Libraries\n\n"
            "The modules under `lib/` contain files derived from the following\n"
            "upstream projects. Their Apache-2.0 license headers are restored\n"
            "verbatim on every derived file (see `tools/cleanup/`).\n\n"
            "### FlorisBoard\n"
            "- Upstream: https://github.com/florisboard/florisboard\n"
            f"- Pinned ref: tag v0.4.6, commit {args.pin_floris}\n"
            "- License: Apache-2.0 (headers restored on derived files)\n"
            "- Derived paths: lib/android, lib/color, lib/compose, lib/kotlin, lib/snygg\n\n"
            "### jetpref\n"
            "- Upstream: https://github.com/patrickgold/jetpref\n"
            f"- Pinned ref: tag v0.3.1, commit {args.pin_jetpref}\n"
            "- License: Apache-2.0 (headers restored on derived files)\n"
            "- Derived paths: lib/jetpref\n"
        )
        marker_start = "## Vendored / Derived Source Libraries"
        if marker_start in content:
            pre = content[:content.index(marker_start)]
            post = content[content.index(marker_start):]
            post_end = post.find("\n## ", 1)
            content = pre + section + (post[post_end + 1:] if post_end != -1 else "")
        else:
            content = content.rstrip() + "\n\n" + section
        notices.write_text(content, encoding="utf-8")
        print(f"notices updated: {notices}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
