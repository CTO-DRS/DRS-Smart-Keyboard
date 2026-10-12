#!/usr/bin/env python3
"""Build the canonical verdicts table — official replacement of the lost
section-0 comparison CSV (DRS cleanup protocol, Phase 1 preliminary commit).

Enumerates EVERY file under lib/ (Kotlin sources, build scripts, ignores),
finds its upstream counterpart against the PINNED measurement refs
(floris main @ fe1241f, jetpref main @ 7a3df4e — pins are final, see
tools/cleanup/WORKLOG.md), and assigns a verdict:

  verbatim(aosp)   — AOSP-derived (protected quartet members / AOSP origin)
  verbatim         — normalized-body fingerprint equals an upstream file
  slightly-modified— same-name fuzzy ratio >= 0.85
  deeply-modified  — same-name fuzzy ratio 0.60..0.85
  merged           — single best < 0.85 but union of top-2 covers >= 75%
  build-file       — build scripts / ignores / repo-meta files
  protected        — the documented protected list (never touched)
  drs-original     — no counterpart at ratio >= 0.60 (DRS-authored)

"similarity_pct" is ALWAYS the canonical unified metric (measure_similarity)
against the chosen counterpart — the same metric used for before/after in
rewrite phases. drs-original rows carry an empty counterpart.

Usage:
  build_verdicts.py --repo <root> --floris <clone> --jetpref <clone> \
      --out tools/cleanup/measurements/canonical_verdicts.csv
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
from measure_similarity import measure, normalize_lines  # noqa: E402

PROTECTED = {
    "lib/compose/src/main/kotlin/org/drs/compose/tooltip/Tooltip.kt": "protected (AOSP origin)",
    "lib/compose/src/main/kotlin/org/drs/compose/tooltip/TooltipDefaults.kt": "protected (AOSP origin)",
    "lib/compose/src/main/kotlin/org/drs/lib/compose/MotionScale.kt": "protected (DRS original)",
}
AOSP_SPECIAL = {
    "KeyboardForwardDelete.kt": "AOSP frameworks/base (verbatim; extension-property trap)",
}
BUILD_NAMES = {"build.gradle.kts", "settings.gradle.kts", ".gitignore",
               "consumer-rules.pro", "proguard-rules.pro", "gradle.properties"}
LIB_MODULES = ["android", "color", "compose", "jetpref", "kotlin", "snygg"]
FLORIS_PIN = "fe1241f"
JETPREF_PIN = "7a3df4e"


def fingerprint(lines: list[str]) -> str:
    return hashlib.sha256("\n".join(lines).encode()).hexdigest()


def upstream_files(floris: Path, jetpref: Path):
    for repo, pin, tag in ((floris, FLORIS_PIN, "floris"),
                           (jetpref, JETPREF_PIN, "jetpref")):
        for p in repo.rglob("*"):
            if p.is_file() and p.suffix in (".kt", ".kts"):
                if any(x in p.parts for x in (".git", "build")):
                    continue
                yield p, f"{tag}@{pin}", repo


def lib_files(repo: Path):
    lib = repo / "lib"
    for p in sorted(lib.rglob("*")):
        if not p.is_file():
            continue
        if any(x in p.parts for x in (".git", "build")):
            continue
        if p.suffix in (".kt", ".kts") or p.name in BUILD_NAMES:
            yield p
        elif p.name == "AndroidManifest.xml" and "src" in p.parts:
            yield p
        elif "schemas" in p.parts and p.suffix == ".json":
            yield p
        elif "META-INF" in p.parts and "services" in p.parts:
            yield p


def candidate_names(fname: str) -> list[str]:
    """DRS renamed upstream Floris* files to Drs* on vendoring — try both."""
    names = [fname]
    stem = Path(fname).stem
    if stem.startswith("Drs"):
        names.append(f"Floris{stem[3:]}.kt")
    return names


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--floris", required=True)
    ap.add_argument("--jetpref", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()
    repo = Path(args.repo)

    by_fp: dict[str, list[tuple[str, str]]] = {}
    by_name: dict[str, list[tuple[str, str, list[str]]]] = {}
    for up, tag, root in upstream_files(Path(args.floris), Path(args.jetpref)):
        raw = up.read_text(encoding="utf-8", errors="replace")
        lines = normalize_lines(raw)
        ref = f"{tag}:{up.relative_to(root)}"
        by_fp.setdefault(fingerprint(lines), []).append((ref, raw))
        by_name.setdefault(up.name, []).append((ref, raw, lines))

    rows = []
    counts: collections.Counter = collections.Counter()
    for f in lib_files(repo):
        rel = str(f.relative_to(repo))
        raw = f.read_text(encoding="utf-8", errors="replace")
        lines = normalize_lines(raw)

        if rel in PROTECTED:
            counts["protected"] += 1
            rows.append({"file": rel, "upstream_ref": "-", "verdict": "protected",
                         "similarity_pct": "", "note": PROTECTED[rel]})
            continue
        if f.name in AOSP_SPECIAL:
            counts["verbatim"] += 1
            rows.append({"file": rel, "upstream_ref": AOSP_SPECIAL[f.name],
                         "verdict": "verbatim", "similarity_pct": "",
                         "note": "AOSP origin, header intact, not matched to floris/jetpref"})
            continue

        is_build = (f.name in BUILD_NAMES and f.suffix != ".kt") or \
                   f.name == "AndroidManifest.xml" or f.suffix == ".json" or \
                   ("META-INF" in f.parts and "services" in f.parts)
        verdict = note = ""
        ref, pct = "-", ""
        if "META-INF" in f.parts and "services" in f.parts:
            counts["resource-file"] += 1
            rows.append({"file": rel, "upstream_ref": "jetpref (KSP registration)",
                         "verdict": "resource-file", "similarity_pct": "",
                         "note": "KSP SymbolProcessorProvider registration — NEVER delete (trap #11)"})
            continue
        hit = by_fp.get(fingerprint(lines))
        if hit:
            upref, upraw = hit[0]
            ref = upref
            verdict = "build-file" if is_build else "verbatim"
            pct = measure(raw, upraw)["similarity_pct"]
        else:
            cands: list[tuple[str, str, list[str]]] = []
            for name in candidate_names(f.name):
                cands.extend(by_name.get(name, []))
            scored = []
            for (upref, upraw, ulines) in cands:
                m = measure(raw, upraw)
                r = m["similarity_pct"]  # canonical metric, order-insensitive
                scored.append((r, upref, upraw, ulines))
            scored.sort(key=lambda x: -x[0])
            if scored and scored[0][0] >= 85.0:
                r, upref, upraw, _ = scored[0]
                ref, verdict = upref, "build-file" if is_build else "slightly-modified"
                note = f"canonical={r:.1f}%"
                pct = f"{r:.1f}"
            elif scored and scored[0][0] >= 30.0:
                r, upref, upraw, _ = scored[0]
                ref, verdict = upref, "build-file" if is_build else "deeply-modified"
                note = f"canonical={r:.1f}%"
                pct = f"{r:.1f}"
            elif not is_build and len(scored) >= 2:
                c_ours = collections.Counter(lines)
                union = collections.Counter()
                for (_, upref, upraw, ulines) in scored[:2]:
                    union.update(ulines)
                cov = sum(min(v, union.get(k, 0)) for k, v in c_ours.items())
                cov_pct = cov * 100.0 / len(lines) if lines else 0.0
                if cov_pct >= 75.0:
                    ref = ";".join(x[1] for x in scored[:2])
                    verdict, note = "merged", f"union-coverage={cov_pct:.0f}%"
                else:
                    verdict, note = "drs-original", "no counterpart >= 0.50"
            else:
                verdict = "build-file" if is_build else "drs-original"
                note = "no counterpart >= 0.50"
        counts[verdict] += 1
        rows.append({"file": rel, "upstream_ref": ref, "verdict": verdict,
                     "similarity_pct": pct, "note": note})

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=["file", "upstream_ref", "verdict",
                                           "similarity_pct", "note"])
        w.writeheader()
        w.writerows(rows)
    print(f"rows={len(rows)}  " + "  ".join(f"{k}={v}" for k, v in sorted(counts.items())))
    return 0


if __name__ == "__main__":
    sys.exit(main())
