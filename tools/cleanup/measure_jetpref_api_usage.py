#!/usr/bin/env python3
"""Jetpref API-surface usage measurement — DRS cleanup protocol (jetpref gate 1).

READ-ONLY measurement tool. Emits:
  measurements/jetpref_api_usage.csv       per-API-symbol usage table
  measurements/jetpref_api_usage_stats.txt totals, top-10, per-module breakdown

Method (documented, deterministic):
  1. Inventory: parse every TOP-LEVEL declaration (brace-depth 0 at line start)
     in lib/jetpref/{datastore-model,datastore-ui,material-ui} (consumer-facing
     modules). Local declarations inside function/class bodies are NOT part of
     the importable API and are excluded. datastore-model-processor is a
     KSP-only unit: it exposes no importable runtime API and is inventoried
     separately in the report (verified: no consumer imports any package under
     it; the trigger annotation @Preferences lives in datastore-model's
     org.drs.jetpref.datastore.annotations package, which IS inventoried).
     Symbol kinds: class/interface/object/enum/typealias/fun/property/
     annotation/composable/extension. Generic receivers are handled
     (e.g. fun <V> PreferenceData<V>.collectAsState()).
     Rows are AGGREGATED by (defining_package, symbol): Kotlin overloads that
     share one importable name form one API surface unit and are reported as
     one row with a declaration count.
  2. Consumer scan: app/src + benchmark + lib/android + lib/color + lib/compose
     (owner scope) plus lib/kotlin + lib/snygg (extra coverage, flagged).
     Comments and string-literal contents stripped with the same string-aware
     scanner contract as measure_similarity.py before any counting.
  3. Reference resolution per file (first match wins):
     a. explicit import  org.drs.jetpref.<pkg>.<Sym>            -> Sym
        (import ... as Alias additionally maps Alias occurrences -> Sym and
         suppresses plain-name counting for Sym in that file)
     b. fully-qualified use org.drs.jetpref.<pkg>.<Sym> outside import lines
        -> Sym (if <pkg>.<Sym> is not a top-level symbol but <pkg>.<Parent> is,
        the reference is member access through that type: credited to Parent)
     c. file explicitly imports Sym (or its container)          -> whole-word
        occurrences of the simple name count as call sites
     d. wildcard import org.drs.jetpref.<pkg>.*                 -> simple-name
        scan for every inventory symbol of that package (defensive; verified:
        no wildcard jetpref imports exist in the tree)
  4. usage_sites = distinct lines where the symbol is referenced (import line,
     simple-name code occurrence or FQ occurrence — a line counts once per
     symbol even if both forms appear). files = distinct consumer files.

Exit code 0 always (measurement, not a gate).
"""
from __future__ import annotations

import argparse
import csv
import re
from collections import defaultdict
from pathlib import Path

JETPREF_PREFIX = "org.drs.jetpref"
INVENTORY_MODULES = {
    "lib/jetpref/datastore-model": "datastore-model",
    "lib/jetpref/datastore-ui": "datastore-ui",
    "lib/jetpref/material-ui": "material-ui",
}
CONSUMER_SCOPE = {  # path-prefix -> scope tag
    "app/src": "app/src",
    "benchmark": "benchmark",
    "lib/android": "lib/android",
    "lib/color": "lib/color",
    "lib/compose": "lib/compose",
    "lib/kotlin": "lib/kotlin(extra)",
    "lib/snygg": "lib/snygg(extra)",
}

DECL_RE = re.compile(
    r"^\s*(?:@\w[\w.]*\s+)*?"
    r"((?:public|internal|private|protected|open|final|abstract|sealed|enum|data|value|inline|suspend|operator|external|infix|expect|actual|companion|annotation)\s+)*"
    r"(class|interface|object|fun|val|var|typealias)\s+"
)
RECEIVER_FUN_RE = re.compile(r"^fun\s+(?:<[^<>]*>\s+)?([A-Za-z_][\w.]*)\s*\.\s*([A-Za-z_][A-Za-z0-9_]*)")
RECEIVER_VAL_RE = re.compile(r"^(?:val|var)\s+(?:<[^<>]*>\s+)?([A-Za-z_][\w.]*)\s*\.\s*([A-Za-z_][A-Za-z0-9_]*)")
IMPORT_RE = re.compile(r"^import\s+([\w.]+)(?:\.\*)?(?:\s+as\s+(\w+))?\s*$")
FQ_RE = re.compile(rf"\b({re.escape(JETPREF_PREFIX)}(?:\.[A-Za-z_][A-Za-z0-9_]*)+)")
GENERIC_BLOCK_RE = re.compile(r"<[^<>]*>")
IDENT_RE = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")
WORD_RE_CACHE: dict[str, re.Pattern] = {}
KEEP_PENDING_PREFIXES = ("(", "where", "private", "internal", "public", "protected", "open",
                         "final", "abstract", "sealed", "enum", "data", "value", "inline",
                         "suspend", "operator", "external", "infix", "expect", "actual",
                         "companion", "annotation", "class", "interface", "object", "fun",
                         "val", "var", "typealias", "@")


def strip_comments(text: str) -> str:
    """String-aware comment stripper (same contract as measure_similarity.py)."""
    out: list[str] = []
    i, n = 0, len(text)
    in_block = in_line = in_str = in_tstr = False
    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if in_block:
            if c == "*" and nxt == "/":
                in_block = False
                i += 2
                continue
            if c == "\n":
                out.append(c)
            i += 1
            continue
        if in_line:
            if c == "\n":
                in_line = False
                out.append(c)
            i += 1
            continue
        if in_str:
            if c == "\\":
                i += 2
                continue
            if c == '"':
                in_str = False
            i += 1
            continue
        if in_tstr:
            if c == "\\":
                i += 2
                continue
            if c == '"' and text[i - 1] == '"' and text[i - 2] == '"':
                in_tstr = False
            i += 1
            continue
        if c == "/" and nxt == "*":
            in_block = True
            i += 2
            continue
        if c == "/" and nxt == "/":
            in_line = True
            i += 2
            continue
        if c == '"':
            if text[i : i + 3] == '"""':
                in_tstr = True
                i += 3
                continue
            in_str = True
            i += 1
            continue
        out.append(c)
        i += 1
    return "".join(out)


def strip_strings(text: str) -> str:
    """Replace string-literal contents with spaces (keep line structure)."""
    out: list[str] = []
    i, n = 0, len(text)
    in_str = in_tstr = False
    while i < n:
        c = text[i]
        if in_tstr:
            if c == "\\":
                out.append(" ")
                i += 2
                continue
            if text[i : i + 3] == '"""':
                in_tstr = False
                out.append('"""')
                i += 3
                continue
            out.append(c if c == "\n" else " ")
            i += 1
            continue
        if in_str:
            if c == "\\":
                out.append(" ")
                i += 2
                continue
            if c == '"':
                in_str = False
                out.append('"')
            elif c == "\n":
                in_str = False
                out.append(c)
            else:
                out.append(" ")
            i += 1
            continue
        if c == '"':
            if text[i : i + 3] == '"""':
                in_tstr = True
                out.append('"""')
                i += 3
                continue
            in_str = True
            out.append('"')
            i += 1
            continue
        out.append(c)
        i += 1
    return "".join(out)


def extract_name(rest: str, kind: str) -> tuple[str, bool] | None:
    """Return (name, is_extension) for a fun/val/var declaration tail (after
    the `fun`/`val`/`var` keyword). Handles generic receivers and type params:
      <V : Any> PreferenceData<V>.collectAsState(): State<V>  -> (collectAsState, True)
      PreferenceGroup(scope: ...)                             -> (PreferenceGroup, False)
      listPrefValue by listPref.collectAsState()              -> (listPrefValue, False)
    """
    s = rest.strip()
    # generic prefix (type params)
    while s.startswith("<"):
        g = re.match(r"^<[^<>]*>\s*", s)
        if not g:
            return None
        s = s[g.end():]
    # cut at the first structural boundary outside generics
    cut = len(s)
    for idx, ch in enumerate(s):
        if ch in "({=:":
            cut = idx
            break
    prefix = s[:cut]
    prefix = GENERIC_BLOCK_RE.sub("", prefix)
    idents = IDENT_RE.findall(prefix)
    if not idents:
        return None
    name = idents[-1]
    # extension only when a dot separates receiver and name inside the prefix
    is_ext = bool(re.search(r"\.[A-Za-z_][A-Za-z0-9_]*$", prefix.strip()))
    return name, is_ext


def parse_inventory(repo: Path) -> list[dict]:
    symbols: list[dict] = []
    for modpath, unit in INVENTORY_MODULES.items():
        for f in sorted((repo / modpath).rglob("*.kt")):
            fp = f.as_posix()
            if "/build/" in fp or "/test/" in fp:
                continue
            rel = f.relative_to(repo).as_posix()
            code = strip_comments(f.read_text(encoding="utf-8"))
            lines = code.splitlines()
            pkg = ""
            pending: list[str] = []
            depth = 0
            for ln in lines:
                s = ln.strip()
                line_start_depth = depth
                depth += ln.count("{") - ln.count("}")
                if s.startswith("package "):
                    pkg = s.split(None, 1)[1].split()[0]
                    pending = []
                    continue
                if not s:
                    continue
                if line_start_depth > 0:
                    # declaration inside a body: not part of the importable API
                    if not s.startswith("@") and not s.startswith(KEEP_PENDING_PREFIXES):
                        pending = []
                    continue
                if s.startswith("@"):
                    pending.append(s)
                    continue
                m = DECL_RE.match(ln)
                if not m:
                    if not s.startswith(KEEP_PENDING_PREFIXES):
                        pending = []
                    continue
                kind = m.group(2)
                mods = (m.group(1) or "").split()
                vis = next((mv for mv in mods if mv in ("public", "internal", "private", "protected")), "public")
                rest = ln[m.end():]
                annos = " ".join(pending)
                pending = []
                if kind in ("class", "interface", "object", "typealias"):
                    if kind == "object" and re.search(r"\bcompanion\b", ln):
                        continue
                    nm = re.match(r"([A-Za-z_][A-Za-z0-9_]*)", rest.strip())
                    if not nm:
                        continue
                    name, symkind = nm.group(1), kind
                    if kind == "class":
                        if re.search(r"annotation\s+class\b", ln):
                            symkind = "annotation"
                        elif "enum" in mods or re.search(r"\benum\s+class\b", ln):
                            symkind = "enum-class"
                        elif "data" in mods:
                            symkind = "data-class"
                        elif "value" in mods:
                            symkind = "value-class"
                    symbols.append(_rec(name, symkind, vis, unit, pkg, rel, annos))
                elif kind == "fun":
                    rest_s = rest.strip()
                    if rest_s.startswith("interface"):  # fun interface X
                        nm = re.match(r"interface\s+([A-Za-z_][A-Za-z0-9_]*)", rest_s)
                        if nm:
                            symbols.append(_rec(nm.group(1), "interface", vis, unit, pkg, rel, annos))
                        continue
                    got = extract_name(rest, "fun")
                    if not got:
                        continue
                    name, is_ext = got
                    symkind = "extension-fun" if is_ext else "fun"
                    if "operator" in mods:
                        symkind += "-operator"
                    if re.search(r"@Composable\b", annos):
                        symkind = "composable-" + symkind if not symkind.startswith("composable") else symkind
                    symbols.append(_rec(name, symkind, vis, unit, pkg, rel, annos))
                else:  # val / var
                    got = extract_name(rest, "val")
                    if not got:
                        continue
                    name, is_ext = got
                    symkind = "extension-property" if is_ext else "property"
                    if re.search(r"@Composable\b", annos):
                        symkind = "composable-" + symkind
                    symbols.append(_rec(name, symkind, vis, unit, pkg, rel, annos))
    return symbols


def _rec(name, kind, vis, unit, pkg, file, annos):
    return {
        "symbol": name,
        "type": kind,
        "visibility": vis,
        "unit": unit,
        "package": pkg,
        "file": file,
        "fqn": f"{pkg}.{name}" if pkg else name,
        "annotations": annos[:80],
    }


def parse_consumers(repo: Path) -> list[dict]:
    files = []
    for prefix in CONSUMER_SCOPE:
        p = repo / prefix
        if p.exists():
            files.extend(f for f in p.rglob("*.kt") if "/build/" not in f.as_posix())
    files = sorted(set(files))
    out = []
    for f in files:
        rel = f.relative_to(repo).as_posix()
        scope = next(v for k, v in CONSUMER_SCOPE.items() if rel.startswith(k + "/") or rel == k)
        code = strip_comments(f.read_text(encoding="utf-8"))
        out.append({
            "file": rel,
            "scope": scope,
            "code_nostr": strip_strings(code),
            "code": code,
        })
    return out


def word_re(name: str) -> re.Pattern:
    r = WORD_RE_CACHE.get(name)
    if r is None:
        r = re.compile(rf"(?<![A-Za-z0-9_]){re.escape(name)}(?![A-Za-z0-9_])")
        WORD_RE_CACHE[name] = r
    return r


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True)
    ap.add_argument("--out-dir", required=True)
    args = ap.parse_args()
    repo = Path(args.repo).resolve()
    outdir = Path(args.out_dir).resolve()
    outdir.mkdir(parents=True, exist_ok=True)

    decls = parse_inventory(repo)
    # aggregate declarations into API-surface rows keyed by (package, symbol)
    rows: dict[tuple[str, str], dict] = {}
    for d in decls:
        key = (d["package"], d["symbol"])
        r = rows.get(key)
        if r is None:
            r = {
                "key": key, "symbol": d["symbol"], "types": {d["type"]},
                "vis": {d["visibility"]}, "units": {d["unit"]},
                "package": d["package"], "decls": 0, "fqn": d["fqn"],
            }
            rows[key] = r
        r["types"].add(d["type"])
        r["vis"].add(d["visibility"])
        r["units"].add(d["unit"])
        r["decls"] += 1
    for r in rows.values():
        r["type"] = "+".join(sorted(r["types"]))
        r["visibility"] = "public" if "public" in r["vis"] else sorted(r["vis"])[0]
        r["defining_unit"] = "+".join(sorted(r["units"]))
        r["usage_sites"] = 0
        r["files"] = set()
        r["scopes"] = set()

    by_fqn: dict[str, dict] = {r["fqn"]: r for r in rows.values()}
    by_name: dict[str, list[dict]] = defaultdict(list)
    for r in rows.values():
        by_name[r["symbol"]].append(r)

    consumers = parse_consumers(repo)
    fq_only_files: list[str] = []
    scope_counts: dict[str, int] = defaultdict(int)
    import_lines_total = 0

    for cfile in consumers:
        code = cfile["code"]
        code_nostr = cfile["code_nostr"]
        body_lines = code_nostr.splitlines()
        imported: dict[str, list[dict]] = defaultdict(list)   # simple/alias name -> rows
        alias_of: dict[str, str] = {}                         # alias -> original symbol
        aliased_symbols: set[str] = set()
        wildcard_pkgs: set[str] = set()
        file_rows: set[tuple[str, str]] = set()
        fq_hits: dict[str, set[int]] = defaultdict(set)

        # (a) explicit imports
        import_line_idx: set[int] = set()
        nonimport_lines: list[str] = []
        for idx, ln in enumerate(body_lines):
            s = ln.strip()
            if s.startswith("import "):
                import_line_idx.add(idx)
                im = IMPORT_RE.match(s)
                if not im or not im.group(1).startswith(JETPREF_PREFIX):
                    continue
                import_lines_total += 1
                path, alias = im.group(1), im.group(2)
                if path.endswith(".*"):
                    wildcard_pkgs.add(path[:-2])
                    continue
                row = by_fqn.get(path)
                if row is None:
                    parent = by_fqn.get(path.rpartition(".")[0])
                    if parent is not None:  # member access through an object/class
                        row = parent
                if row is not None:
                    file_rows.add(row["key"])
                    imported[alias or row["symbol"]].append(row)
                    if alias:
                        alias_of[alias] = row["symbol"]
                        aliased_symbols.add(row["symbol"])
                continue
            nonimport_lines.append(ln)

        # (b) fully-qualified references outside import lines
        for ln_i, ln in enumerate(body_lines):
            if ln_i in import_line_idx:
                continue
            for m in FQ_RE.finditer(ln):
                path = m.group(1)
                row = by_fqn.get(path)
                if row is None:
                    row = by_fqn.get(path.rpartition(".")[0])
                if row is None:
                    last = path.rpartition(".")[2]
                    cands = [x for x in by_name.get(last, []) if path.endswith("." + x["package"])]
                    row = cands[0] if len(cands) == 1 else None
                if row is not None:
                    file_rows.add(row["key"])
                    fq_hits[row["fqn"]].add(ln_i)

        # (c) simple-name occurrences for explicitly imported symbols
        simple_hits: dict[tuple[str, str], set[int]] = defaultdict(set)
        for name, rws in imported.items():
            wr = word_re(name)
            for row in rws:
                if row["symbol"] in aliased_symbols and name == row["symbol"]:
                    continue  # aliased: original name no longer refers to it
                for i, ln in enumerate(body_lines):
                    if wr.search(ln):
                        simple_hits[row["key"]].add(i)
                        file_rows.add(row["key"])

        # (d) wildcard packages: scan every inventory symbol of that package
        for wp in wildcard_pkgs:
            for row in rows.values():
                if row["package"] != wp:
                    continue
                wr = word_re(row["symbol"])
                for i, ln in enumerate(body_lines):
                    if wr.search(ln):
                        simple_hits[row["key"]].add(i)
                        file_rows.add(row["key"])

        # commit per-file usage (a line counts once per row)
        for key in file_rows:
            row = rows[key]
            union = set(simple_hits.get(key, set()))
            for i in fq_hits.get(row["fqn"], set()):
                union.add(i)
            alias_used = next((a for a, o in alias_of.items() if o == row["symbol"]), None)
            if alias_used is not None:
                wr = word_re(alias_used)
                union |= {i for i, ln in enumerate(body_lines) if wr.search(ln)}
            if not union:
                union = {next(iter(import_line_idx))} if import_line_idx else {0}
            row["usage_sites"] += len(union)
            row["files"].add(cfile["file"])
            row["scopes"].add(cfile["scope"])

        if file_rows:
            scope_counts[cfile["scope"]] += 1
        if fq_hits:
            fq_only_files.append(cfile["file"])

    # CSV output (sorted: defining_unit, package, symbol)
    csv_path = outdir / "jetpref_api_usage.csv"
    out_rows = []
    for r in sorted(rows.values(), key=lambda r: (r["defining_unit"], r["package"], r["symbol"])):
        out_rows.append({
            "symbol": r["symbol"],
            "type": r["type"],
            "visibility": r["visibility"],
            "defining_unit": r["defining_unit"],
            "defining_package": r["package"],
            "declarations": r["decls"],
            "usage_sites": r["usage_sites"],
            "files": ";".join(sorted(r["files"])),
        })
    with csv_path.open("w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=list(out_rows[0].keys()))
        w.writeheader()
        w.writerows(out_rows)

    # Stats
    public_rows = [r for r in rows.values() if r["visibility"] == "public"]
    used = [r for r in rows.values() if r["files"]]
    used_public = [r for r in used if r["visibility"] == "public"]
    top = sorted(rows.values(), key=lambda r: (-r["usage_sites"], r["symbol"]))[:10]
    total_decls = sum(r["decls"] for r in rows.values())
    lines = []
    a = lines.append
    a("Jetpref API usage measurement (jetpref gate 1) — read-only")
    a("=" * 60)
    a(f"inventory_api_rows_total     : {len(rows)}  (unique defining_package+symbol; declarations={total_decls})")
    a(f"inventory_rows_public        : {len(public_rows)}")
    a(f"symbols_used_total           : {len(used)}")
    a(f"symbols_used_public          : {len(used_public)}")
    a(f"usage_ratio_total            : {len(used)}/{len(rows)} = {len(used)*100/max(1,len(rows)):.1f}%")
    a(f"usage_ratio_public           : {len(used_public)}/{len(public_rows)} = {len(used_public)*100/max(1,len(public_rows)):.1f}%")
    a(f"consumer_files_scanned       : {len(consumers)}")
    a(f"consumer_files_referencing   : {sum(scope_counts.values())}")
    a(f"jetpref_import_lines_total   : {import_lines_total}")
    a(f"files_using_fq_references    : {len(fq_only_files)} (FQ org.drs.jetpref.* outside import lines)")
    for f in sorted(fq_only_files):
        a(f"  - {f}")
    a("")
    a("files referencing jetpref, by scope:")
    for k in sorted(scope_counts):
        a(f"  {k:24s}: {scope_counts[k]}")
    a("")
    a("top 10 most-used symbols:")
    for r in top:
        a(f"  {r['symbol']:34s} {r['type']:26s} {r['defining_unit']:28s} sites={r['usage_sites']:4d} files={len(r['files'])}")
    a("")
    unused = sorted((r for r in rows.values() if not r["files"]), key=lambda r: (r["defining_unit"], r["package"], r["symbol"]))
    a(f"unused api rows ({len(unused)}):")
    for r in unused:
        a(f"  {r['defining_unit']:16s} {r['package']:52s} {r['symbol']} ({r['type']},{r['visibility']})")
    stats_path = outdir / "jetpref_api_usage_stats.txt"
    stats_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"inventory_rows={len(rows)} used={len(used)} -> {csv_path}")
    print(f"stats -> {stats_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
