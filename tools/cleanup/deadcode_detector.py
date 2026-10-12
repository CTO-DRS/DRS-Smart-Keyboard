#!/usr/bin/env python3
"""Project-wide dead-code detector — DRS cleanup protocol (Phase 2 tool).

Methodology (identical to the approved Phase-2 round):
  Stage 1 — file level: for every lib/*/src/main source file, count
  references to its stem across the WHOLE project (app + lib + tests +
  benchmark + build files + resources). Zero references => candidate.
  Stage 2 — symbol level: for surviving files, extract top-level
  declarations and count references the same way. Zero => candidate.

Documented traps baked in (do NOT re-discover them):
  T1  Extension properties: `val Receiver.name` is referenced by `name`
      alone (Icons.AutoMirrored.Filled.ForwardDelete case) — matched by
      property name, not receiver.
  T2  KSP/META-INF/services: any class registered in a META-INF/services
      file is USED (ModelProcessor case), plus any annotation whose FQCN
      appears in a services file.
  T3  Test entry-point classes are discovered by runners without code
      references — files under src/test|androidTest|benchmark are never
      candidates, and references FROM tests always count (project-wide
      corpus includes tests per protocol rule 9).
  T4  build.gradle.kts / settings references and @Composable preview
      usage count via the same corpus (includes .kts, .xml).
  T5  This tool NEVER deletes. The compiler is the final arbiter: every
      candidate must be confirmed by a real build+test gate before and
      after deletion (rollback on any failure).

Usage:
  deadcode_detector.py --repo <repo-root> [--out report.csv] [--markdown]
"""
from __future__ import annotations

import argparse
import csv
import re
import subprocess
import sys
from pathlib import Path

DECL = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s+)*"
    r"(?:public\s+|private\s+|internal\s+|protected\s+|open\s+|abstract\s+|"
    r"final\s+|sealed\s+|data\s+|value\s+|inline\s+|fun\s+interface\s+|"
    r"external\s+|suspend\s+|operator\s+|const\s+|lateinit\s+)*"
    r"(fun|val|var|class|object|interface)\b\s*"
    r"(?:<[^>]*>\s*)?([`A-Za-z_][`A-Za-z0-9_]*)")
EXT_PROP = re.compile(r"^val\s+[\w.`?]+\.(?:`?)([A-Za-z_][A-Za-z0-9_]*)(?:`?)\s*[:<(]")


def tracked_sources(repo: Path) -> list[Path]:
    out = subprocess.run(
        ["git", "-C", str(repo), "ls-files", "-z"],
        check=True, capture_output=True).stdout
    paths = []
    for raw in out.decode().split("\0"):
        if not raw:
            continue
        p = Path(raw)
        if p.suffix in (".kt", ".kts", ".xml") or "META-INF" in p.parts:
            paths.append(repo / p)
    return paths


def is_test_source(rel: str) -> bool:
    parts = rel.split("/")
    return any(s in parts for s in ("test", "androidTest", "benchmark"))


def is_main_lib_candidate(rel: str) -> bool:
    parts = rel.split("/")
    return (len(parts) > 4 and parts[0] == "lib"
            and "src" in parts and "main" in parts
            and parts[-1].endswith((".kt", ".kts")))


def package_of(text: str) -> str:
    m = re.search(r"^\s*package\s+([\w.]+)", text, re.M)
    return m.group(1) if m else ""


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--repo", required=True)
    ap.add_argument("--out")
    ap.add_argument("--markdown")
    args = ap.parse_args()
    repo = Path(args.repo)

    files = tracked_sources(repo)
    texts: dict[Path, str] = {}
    for p in files:
        try:
            texts[p] = p.read_text(encoding="utf-8", errors="replace")
        except OSError:
            pass

    corpus = "\n".join(texts.values())
    services_blob = "\n".join(t for p, t in texts.items()
                              if "META-INF" in p.parts and "services" in p.parts)

    rows: list[dict] = []
    candidates = [p for p, rel in
                  ((p, str(p.relative_to(repo))) for p in texts)
                  if is_main_lib_candidate(rel)]
    for cand in candidates:
        rel = str(cand.relative_to(repo))
        text = texts[cand]
        stem = cand.stem
        pkg = package_of(text)
        fqcn = f"{pkg}.{stem}" if pkg else stem

        others = corpus
        # subtract own file's text from the corpus for counting
        own_count = text.count(stem)
        total_stem = len(re.findall(rf"\b{re.escape(stem)}\b", corpus))
        refs = total_stem - own_count if stem in text else total_stem

        exempt_services = fqcn in services_blob or stem in services_blob
        if refs == 0 and not exempt_services:
            rows.append({"file": rel, "kind": "file", "name": stem,
                         "refs": 0, "exempt": "no", "note": "delete candidate"})
        elif refs == 0 and exempt_services:
            rows.append({"file": rel, "kind": "file", "name": stem,
                         "refs": 0, "exempt": "services", "note": "KSP-registered, keep"})

        # Stage 2: top-level symbols in files that themselves are referenced
        if refs > 0 or exempt_services:
            for line in text.splitlines():
                if line.startswith((" ", "\t")) or line.strip().startswith(("@", "//", "*", "/*")):
                    # still allow annotations-attached decls: fallthrough below
                    pass
                m = EXT_PROP.match(line.strip())
                if m:
                    name = m.group(1)
                    n_own = len(re.findall(rf"\b{re.escape(name)}\b", text))
                    n_all = len(re.findall(rf"\b{re.escape(name)}\b", corpus))
                    if n_all - n_own == 0:
                        rows.append({"file": rel, "kind": "ext-prop",
                                     "name": name, "refs": 0, "exempt": "no",
                                     "note": "orphan extension property"})
                    continue
                if re.match(r"^(?:public|private|internal|protected|open|abstract|final|sealed|data|value|inline|external|suspend|operator|const|lateinit|fun|val|var|class|object|interface|@)", line.strip()):
                    stripped = line.strip()
                    if not stripped or stripped.startswith(("//", "/*", "*", "import", "package")):
                        continue
                    m2 = DECL.match(stripped)
                    if not m2:
                        continue
                    kw, name = m2.group(1), m2.group(2).strip("`")
                    if kw in ("fun", "val", "var") and "." in line.split(kw, 1)[1][:60]:
                        continue  # extension member handled by EXT_PROP
                    n_own = len(re.findall(rf"\b{re.escape(name)}\b", text))
                    n_all = len(re.findall(rf"\b{re.escape(name)}\b", corpus))
                    if n_all - n_own == 0:
                        rows.append({"file": rel, "kind": f"top-{kw}",
                                     "name": name, "refs": 0, "exempt": "no",
                                     "note": "orphan top-level symbol"})

    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
        with out.open("w", newline="", encoding="utf-8") as fh:
            w = csv.DictWriter(fh, fieldnames=["file", "kind", "name", "refs",
                                               "exempt", "note"])
            w.writeheader()
            w.writerows(rows)
    if args.markdown:
        md = Path(args.markdown)
        md.parent.mkdir(parents=True, exist_ok=True)
        with md.open("w", encoding="utf-8") as fh:
            fh.write("# Dead-code candidates (project-wide scan)\n\n")
            fh.write("Compiler is the final arbiter; gate before/after every "
                     "deletion batch.\n\n")
            for r in rows:
                fh.write(f"- `{r['file']}` — {r['kind']} `{r['name']}` "
                         f"(refs={r['refs']}, exempt={r['exempt']})\n")

    print(f"scanned {len(texts)} tracked source files, "
          f"{len(candidates)} lib main candidates")
    print(f"candidates: {len(rows)}")
    for r in rows[:50]:
        print(f"  {r['kind']:<9} {r['name']:<40} {r['file']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
