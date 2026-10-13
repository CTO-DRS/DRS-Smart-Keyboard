#!/usr/bin/env python3
"""Gate A (Phase 1): assert ZERO code changes — headers only.

For every file changed under lib/ vs HEAD: strip the leading license block
from both the old (HEAD) and new (worktree) content and compare SHA-256 of
the remaining bodies. Any mismatch = gate failure (exit 1).

Usage: gate_bodies.py [--repo .] [--against HEAD]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from restore_headers import body_after_header  # noqa: E402


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", default=".")
    ap.add_argument("--against", default="HEAD")
    args = ap.parse_args()
    repo = Path(args.repo)

    changed = subprocess.run(
        ["git", "-C", str(repo), "diff", "--name-only", args.against, "--", "lib/"],
        check=True, capture_output=True).stdout.decode().split()
    failures = []
    for rel in changed:
        old_raw = subprocess.run(
            ["git", "-C", str(repo), "show", f"{args.against}:{rel}"],
            check=True, capture_output=True).stdout.decode("utf-8", errors="replace")
        new_raw = (repo / rel).read_text(encoding="utf-8", errors="replace")
        old_body = body_after_header(old_raw)
        new_body = body_after_header(new_raw)
        h_old = hashlib.sha256(old_body.encode()).hexdigest()
        h_new = hashlib.sha256(new_body.encode()).hexdigest()
        if h_old != h_new:
            failures.append(rel)
            print(f"BODY CHANGED: {rel}", file=sys.stderr)

    print(f"gate-a: checked={len(changed)} files, violations={len(failures)}")
    if failures:
        return 1
    print("gate-a: PASS — zero body changes (headers only)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
