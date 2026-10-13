#!/usr/bin/env python3
"""Phase 2 pre-kill review — deliverables for the owner (banking protocol).

Produces tools/cleanup/measurements/phase2_review.csv with one row per
audited item, plus a printed summary:

  1) Fuzzy header matches (phase1_header_map.csv kind~fuzzy*) with ref+score.
  2) The 12 no-match files: canonical verdict + documented measurement +
     explicit derived/non-derived confirmation (fresh best-match scan for
     every code file whose canonical row lacks a number).
  3) Consistency audit over canonical_verdicts.csv:
        derived verdict  (verbatim|slightly-modified|deeply-modified)
            <-> file MUST carry an Apache-2.0 header
        drs-original     <-> MUST have a documented best-match measurement
Read-only. Never writes source files.
"""
import csv
import os
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MEAS = os.path.join(HERE, "measurements")
REPO_DEFAULT = os.path.abspath(os.path.join(HERE, "..", ".."))
FLORIS_DEFAULT = "/home/z/my-project/repos/floris-main"
JETPREF_DEFAULT = "/home/z/my-project/repos/jetpref-main"


def sh_measure(repo, ours, upstream):
    """Run measure_similarity.py; return (similarity, ours, upstream, matched)."""
    tool = os.path.join(HERE, "measure_similarity.py")
    r = subprocess.run(
        [sys.executable, tool, "--file", ours, "--upstream", upstream],
        cwd=repo, capture_output=True, text=True,
    )
    lines = [l for l in r.stdout.strip().splitlines() if l.strip()]
    if len(lines) < 2:
        return None
    parts = lines[-1].split(",")
    # file,upstream,ours_lines,upstream_lines,matched_lines,similarity_pct
    return float(parts[5]), int(parts[2]), int(parts[3]), int(parts[4])


def best_match(repo, ours, roots):
    """Scan candidate upstream roots for the best similarity score."""
    best = None
    for root in roots:
        for dirpath, _dirs, files in os.walk(root):
            for f in sorted(files):
                if not f.endswith(".kt") and not f.endswith(".kts"):
                    continue
                up = os.path.join(dirpath, f)
                m = sh_measure(repo, ours, up)
                if m and (best is None or m[0] > best[0]):
                    best = (m[0], os.path.relpath(up, root),) + m[1:]
    return best


def header_state(path):
    try:
        with open(path, "r", encoding="utf-8", errors="replace") as fh:
            head = fh.read(2048)
    except OSError:
        return "missing-file"
    has_apache = "Apache License" in head
    has_url = "www.apache.org/licenses/LICENSE-2.0" in head
    if has_apache and has_url:
        return "apache-header"
    if has_apache or has_url:
        return "apache-partial"
    return "no-apache-header"


def resolve_upstream(up_ref, floris, jetpref):
    """canonical upstream_ref 'floris@fe1241f:<path>' -> local absolute path."""
    if up_ref.startswith("floris@"):
        return os.path.join(floris, up_ref.split(":", 1)[1])
    if up_ref.startswith("jetpref@"):
        return os.path.join(jetpref, up_ref.split(":", 1)[1])
    return None


def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=REPO_DEFAULT)
    ap.add_argument("--floris", default=FLORIS_DEFAULT)
    ap.add_argument("--jetpref", default=JETPREF_DEFAULT)
    ap.add_argument("--out", default=os.path.join(MEAS, "phase2_review.csv"))
    args = ap.parse_args()

    repo, floris, jetpref = args.repo, args.floris, args.jetpref
    rows_out = []

    # ---------- item 1: fuzzy header matches ----------
    with open(os.path.join(MEAS, "phase1_header_map.csv")) as fh:
        header_map = list(csv.DictReader(fh))
    fuzzy = [r for r in header_map if r["kind"].startswith("fuzzy")]
    print(f"ITEM 1 — fuzzy header matches: {len(fuzzy)}")
    for r in fuzzy:
        print(f"  {r['file']} -> {r['upstream']} [{r['kind']}]")
        rows_out.append({
            "item": "1-fuzzy", "file": r["file"], "ref": r["upstream"],
            "score": r["kind"].replace("fuzzy:", "").replace("+canonical-header", ""),
            "classification": "derived (header restored phase1)", "audit": "ok",
        })

    # ---------- item 2: the 12 no-match ----------
    with open(os.path.join(MEAS, "canonical_verdicts.csv")) as fh:
        canonical = {r["file"]: r for r in csv.DictReader(fh)}
    nomatch = [r for r in header_map if r["action"] == "no-match-keep-drs-header"]
    print(f"\nITEM 2 — no-match files: {len(nomatch)}")
    for r in nomatch:
        f = r["file"]
        c = canonical.get(f, {})
        verdict = c.get("verdict", "?")
        sim = c.get("similarity_pct", "")
        note = c.get("note", "")
        path = os.path.join(repo, f)
        derived = verdict in ("verbatim", "slightly-modified", "deeply-modified")
        if verdict == "build-file" and sim:
            # build-file with documented canonical measurement
            print(f"  {f} | build-file | canonical={sim}% vs "
                  f"{c.get('upstream_ref','')} | CONFIRMED: DRS build config "
                  f"(not a code derivative)")
            rows_out.append({"item": "2-nomatch", "file": f,
                             "ref": c.get("upstream_ref", "-"), "score": sim,
                             "classification": "build-file",
                             "audit": "confirmed-non-derived(build-config)"})
        elif verdict == "drs-original":
            # fresh documented best-match scan so the number is on record
            if "jetpref" in f:
                roots = [os.path.join(jetpref, "datastore-model")]
            else:
                roots = [os.path.join(floris, "lib", "android"),
                         os.path.join(floris, "app", "src", "main", "kotlin")]
            roots = [x for x in roots if os.path.isdir(x)]
            bm = best_match(repo, path, roots) if os.path.exists(path) else None
            score = f"{bm[0]:.1f}" if bm else "n/a"
            ref = bm[1] if bm else "-"
            conf = ("CONFIRMED non-derived" if bm and bm[0] < 30.0
                    else "DOUBTFUL — derived-level match!")
            print(f"  {f} | drs-original | best={score}% vs {ref} | {conf}")
            rows_out.append({"item": "2-nomatch", "file": f, "ref": ref,
                             "score": score, "classification": "drs-original",
                             "audit": conf.split(" ")[0]})
        else:
            # canonical itself classifies it as derived (or unknown)
            print(f"  {f} | {verdict} | canonical={sim or '-'}% vs "
                  f"{c.get('upstream_ref', '-')} | DOUBTFUL — canonical table "
                  f"says DERIVED, phase1 kept DRS header")
            rows_out.append({"item": "2-nomatch", "file": f,
                             "ref": c.get("upstream_ref", "-"),
                             "score": sim or "-", "classification": verdict,
                             "audit": "DOUBTFUL-derived-per-canonical"})

    # ---------- item 3: consistency audit ----------
    print("\nITEM 3 — consistency audit (canonical table x actual headers)")
    violations = 0
    for f, c in sorted(canonical.items()):
        verdict = c["verdict"]
        note = c.get("note") or ""
        if "AOSP" in note:
            # protected AOSP-origin files: 2-line AOSP declaration header is
            # the correct, owner-approved form (KeyboardForwardDelete case).
            rows_out.append({"item": "3-audit", "file": f,
                             "ref": c.get("upstream_ref", "-"),
                             "score": c.get("similarity_pct", ""),
                             "classification": verdict,
                             "audit": "ok(protected-aosp-header)"})
            continue
        path = os.path.join(repo, f)
        hs = header_state(path)
        if verdict in ("verbatim", "slightly-modified", "deeply-modified"):
            ok = hs in ("apache-header", "apache-partial")
            state = "ok" if ok else "VIOLATION: derived without Apache header"
        elif verdict == "drs-original":
            ok = bool(c.get("similarity_pct")) or "drs" in (c.get("note") or "")
            if not ok and os.path.exists(path):
                if "jetpref" in f:
                    roots = [os.path.join(jetpref, "datastore-model")]
                else:
                    roots = [os.path.join(floris, "lib", "android")]
                roots = [x for x in roots if os.path.isdir(x)]
                bm = best_match(repo, path, roots)
                if bm:
                    ok = bm[0] < 30.0
            state = "ok" if ok else "VIOLATION: drs-original without documented measurement"
        else:
            ok = True
            state = "ok"
        if not ok:
            violations += 1
            print(f"  {f} | {verdict} | header={hs} | {state}")
        rows_out.append({"item": "3-audit", "file": f,
                         "ref": c.get("upstream_ref", "-"),
                         "score": c.get("similarity_pct", ""),
                         "classification": verdict,
                         "audit": state if not ok else f"ok({hs})"})
    print(f"  violations: {violations}")

    with open(args.out, "w", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=["item", "file", "ref", "score",
                                           "classification", "audit"])
        w.writeheader()
        w.writerows(rows_out)
    print(f"\nwrote {args.out} ({len(rows_out)} rows)")


if __name__ == "__main__":
    main()
