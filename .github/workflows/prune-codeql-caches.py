"""Delete the CodeQL caches that nothing can reach any more.

Reads a cache listing, one JSON object per line, from the file named on the
command line; that is what "gh api --paginate .../actions/caches --jq
.actions_caches[]" writes.  Deletes every listed CodeQL cache that a newer one
shadows, unless the environment variable DRY_RUN is "true".  Names the
repository to delete from in the environment variable GH_REPO.
"""

import json
import operator
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

# A cache is reachable only if it is the newest one whose key matches the
# restore key that CodeQL looks it up by.  Each pattern captures that restore
# key; what follows it varies from one run to the next.
FAMILIES = [
    re.compile(r"^(codeql-dependencies-.*-)[0-9a-f]{64}$"),
    re.compile(r"^(codeql-overlay-base-database-.*-[0-9.]+-)[0-9a-f]{40}-\d+-\d+$"),
]


def restore_key(key: str) -> str | None:
    """Determine which restore key, if any, selects KEY.

    Returns:
        the restore key that selects KEY, or None if KEY is unrecognized.
    """
    for family in FAMILIES:
        match = family.match(key)
        if match:
            return match.group(1)
    return None


def describe(label: str, cache: dict[str, Any]) -> str:
    """Describe CACHE for the log.

    Returns:
        a one-line description of CACHE, marked with LABEL.
    """
    return f"{label:<12}  {cache['size_in_bytes'] / 1e6:8.0f} MB  {cache['key']}"


def partition(listing: Path) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    """Split the caches in LISTING according to whether CodeQL can still find them.

    Returns:
        the reachable caches and the unreachable ones, each sorted by key.
    """
    newest: dict[tuple[str, str, str], dict[str, Any]] = {}
    doomed: list[dict[str, Any]] = []
    seen: set[int] = set()
    with listing.open() as caches:
        for line in caches:
            if not line.strip():
                continue
            cache = json.loads(line)
            # Paging can list a cache twice; deleting the duplicate would
            # delete the cache that this run decided to keep.
            if cache["id"] in seen:
                continue
            seen.add(cache["id"])
            key = restore_key(cache["key"])
            if key is None:
                # An unrecognized key shape might be reachable, so leave it.
                # Report the CodeQL ones: if CodeQL changes its key format,
                # this workflow deletes nothing, and the log is the only place
                # that says so.
                if cache["key"].startswith("codeql-"):
                    print(describe("unrecognized", cache))
                continue
            # A cache is scoped to the ref that wrote it, so the newest cache
            # of a family on one ref does not shadow another ref's.  A restore
            # also matches only caches whose version -- a hash of the paths and
            # the compression method -- equals the one it asks for, so caches
            # that differ in version do not shadow one another either.
            group = (cache["ref"], cache["version"], key)
            previous = newest.get(group)
            if previous is None or cache["created_at"] > previous["created_at"]:
                if previous is not None:
                    doomed.append(previous)
                newest[group] = cache
            else:
                doomed.append(cache)
    by_key = operator.itemgetter("key")
    return sorted(newest.values(), key=by_key), sorted(doomed, key=by_key)


def main() -> None:
    """Delete every CodeQL cache that a newer cache shadows."""
    if len(sys.argv) != 2:
        sys.exit(f"usage: {sys.argv[0]} CACHES.JSONL")
    repo = os.environ["GH_REPO"]
    dry_run = os.environ.get("DRY_RUN") == "true"

    keep, doomed = partition(Path(sys.argv[1]))
    for cache in keep:
        print(describe("keep", cache))

    freed = 0
    deleted = 0
    gone = 0
    failures = 0
    for cache in doomed:
        print(describe("would delete" if dry_run else "delete", cache))
        if dry_run:
            freed += cache["size_in_bytes"]
            continue
        deletion = subprocess.run(
            [
                "gh",
                "api",
                "-X",
                "DELETE",
                "--silent",
                f"repos/{repo}/actions/caches/{cache['id']}",
            ],
            capture_output=True,
            text=True,
            check=False,
        )
        if deletion.returncode == 0:
            deleted += 1
            freed += cache["size_in_bytes"]
        elif "HTTP 404" in deletion.stderr:
            # GitHub evicted the cache between the listing and now, which is
            # likeliest when the quota is full -- the situation this workflow
            # exists to fix.  The cache is gone, which is what this run wanted,
            # so do not fail.  Another run deleting it concurrently looks the
            # same and is equally fine.
            gone += 1
            print(f"ALREADY GONE      {cache['key']}")
        else:
            # Something else, such as rate-limiting.  The other caches are
            # still worth deleting.
            failures += 1
            print(f"FAILED TO DELETE  {cache['key']}: {deletion.stderr.strip()}")

    if dry_run:
        print(f"would delete {len(doomed)} caches, freeing {freed / 1e9:.2f} GB")
    else:
        print(f"deleted {deleted} caches, freeing {freed / 1e9:.2f} GB")
        if gone:
            print(f"{gone} caches were already gone")
    if failures:
        sys.exit(f"{failures} of {len(doomed)} deletions failed")


if __name__ == "__main__":
    main()
