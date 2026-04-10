#!/usr/bin/env python3
"""Create missing Xcode vector imagesets from Compose bundled SVG icons.

Compose layout (source of truth):
    Compose/YABA/app/src/main/assets/files/icons/<name>.svg

Darwin layout (mirrors names; only Contents.json references the SVG filename):
    Darwin/YABA/Assets.xcassets/<name>.imageset/Contents.json
    Darwin/YABA/Assets.xcassets/<name>.imageset/<name>.svg

For each SVG on the Compose side, if `<name>.imageset` is missing under Assets.xcassets,
this script creates the imageset folder, writes Contents.json (preserves-vector-representation),
and copies the SVG. Existing imagesets are left unchanged unless --force is used.

Usage:
    python3 Scripts/sync_compose_icons_to_darwin.py --dry-run
    python3 Scripts/sync_compose_icons_to_darwin.py --verbose
    python3 Scripts/sync_compose_icons_to_darwin.py --report-json /tmp/icon-sync-report.json
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from collections import Counter
from pathlib import Path
from typing import List, Set, Tuple

ROOT = Path(__file__).resolve().parent.parent

DEFAULT_COMPOSE_ICONS = (
    ROOT
    / "Compose"
    / "YABA"
    / "app"
    / "src"
    / "main"
    / "assets"
    / "files"
    / "icons"
)
DEFAULT_DARWIN_ASSETS = ROOT / "Darwin" / "YABA" / "Assets.xcassets"


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument(
        "--compose-icons",
        type=Path,
        dest="compose_icons",
        default=DEFAULT_COMPOSE_ICONS,
        help="Directory containing flat *.svg icons (Compose assets)",
    )
    p.add_argument(
        "--darwin-assets",
        type=Path,
        dest="darwin_assets",
        default=DEFAULT_DARWIN_ASSETS,
        help="Darwin Assets.xcassets root",
    )
    p.add_argument("--dry-run", action="store_true", help="Print planned actions only")
    p.add_argument(
        "--force",
        action="store_true",
        help="Overwrite SVG and rewrite Contents.json when imageset already exists",
    )
    p.add_argument("--verbose", action="store_true")
    p.add_argument(
        "--report-json",
        type=Path,
        help="Write a JSON report (missing copied, darwin-only names, errors)",
    )
    p.add_argument(
        "--check",
        action="store_true",
        help="Exit 1 if any Compose icon has no matching Darwin imageset; no file operations",
    )
    return p.parse_args()


def collect_compose_stems(compose_icons: Path) -> Tuple[Set[str], List[Path]]:
    """Return (stems, svg_paths) for all *.svg files directly under compose_icons."""
    if not compose_icons.is_dir():
        raise SystemExit(f"Not a directory: {compose_icons}")

    paths = sorted(compose_icons.glob("*.svg"))
    stems = [p.stem for p in paths]
    dupes = [s for s, n in Counter(stems).items() if n > 1]
    if dupes:
        raise SystemExit(f"Duplicate SVG stems in Compose icons: {sorted(dupes)}")

    return set(stems), paths


def collect_darwin_image_stems(darwin_assets: Path) -> Set[str]:
    """Stem names from each *.imageset directory at the top level of Assets.xcassets."""
    if not darwin_assets.is_dir():
        raise SystemExit(f"Not a directory: {darwin_assets}")

    stems: Set[str] = set()
    for child in darwin_assets.iterdir():
        if not child.is_dir():
            continue
        name = child.name
        if not name.endswith(".imageset"):
            continue
        stems.add(name[: -len(".imageset")])
    return stems


def contents_json_bytes(svg_filename: str) -> bytes:
    """Match existing YABA asset catalog JSON style (space before colons)."""
    filename_json = json.dumps(svg_filename, ensure_ascii=False)
    text = f"""{{
  "images" : [
    {{
      "filename" : {filename_json},
      "idiom" : "universal"
    }}
  ],
  "info" : {{
    "author" : "xcode",
    "version" : 1
  }},
  "properties" : {{
    "preserves-vector-representation" : true
  }}
}}
"""
    return text.encode("utf-8")


def sync_one(
    stem: str,
    compose_svg: Path,
    darwin_assets: Path,
    *,
    dry_run: bool,
    force: bool,
    verbose: bool,
) -> str:
    """Returns action: 'created', 'skipped', 'updated', or 'error: ...'."""
    imageset = darwin_assets / f"{stem}.imageset"
    dest_json = imageset / "Contents.json"
    svg_name = f"{stem}.svg"

    try:
        if imageset.is_dir() and not force:
            if verbose:
                print(f"  skip (exists): {imageset.name}", file=sys.stderr)
            return "skipped"

        if dry_run:
            if imageset.is_dir() and force:
                print(f"  [dry-run] would refresh: {imageset.name}", file=sys.stderr)
                return "updated"
            print(f"  [dry-run] would create: {imageset.name}", file=sys.stderr)
            return "created"

        imageset.mkdir(parents=True, exist_ok=True)
        shutil.copy2(compose_svg, imageset / f"{stem}.svg")
        dest_json.write_bytes(contents_json_bytes(svg_name))
        if imageset.is_dir() and force:
            return "updated"
        return "created"
    except OSError as e:
        return f"error: {e}"


def main() -> None:
    args = parse_args()
    compose_stems, compose_paths = collect_compose_stems(args.compose_icons)
    compose_by_stem = {p.stem: p for p in compose_paths}
    darwin_stems = collect_darwin_image_stems(args.darwin_assets)

    missing = sorted(compose_stems - darwin_stems)
    darwin_only = sorted(darwin_stems - compose_stems)

    if args.check:
        if missing:
            print(
                f"Check failed: {len(missing)} Compose icon(s) missing under Darwin "
                f"(Assets.xcassets *.imageset).",
                file=sys.stderr,
            )
            raise SystemExit(1)
        print(
            f"OK: all {len(compose_stems)} Compose icon(s) have a matching Darwin imageset.",
            file=sys.stderr,
        )
        raise SystemExit(0)

    actions: List[str] = []
    errors: List[str] = []

    if args.force:
        to_process = sorted(compose_stems)
    else:
        to_process = missing

    print(
        f"Compose icons: {len(compose_stems)} | Darwin imagesets: {len(darwin_stems)} | "
        f"to add/update: {len(to_process)} | Darwin-only (informational): {len(darwin_only)}",
        file=sys.stderr,
    )

    for stem in to_process:
        src = compose_by_stem.get(stem)
        if src is None:
            continue
        action = sync_one(
            stem,
            src,
            args.darwin_assets,
            dry_run=args.dry_run,
            force=args.force,
            verbose=args.verbose,
        )
        actions.append(f"{stem}\t{action}")
        if action.startswith("error"):
            errors.append(f"{stem}: {action}")
        elif args.verbose and not args.dry_run:
            print(f"  {stem}: {action}", file=sys.stderr)

    if args.report_json:
        report = {
            "compose_icon_count": len(compose_stems),
            "darwin_imageset_count": len(darwin_stems),
            "missing_before_sync": missing,
            "darwin_only_stems": darwin_only,
            "processed_stems": to_process,
            "actions": actions,
            "errors": errors,
        }
        text = json.dumps(report, indent=2, ensure_ascii=False)
        args.report_json.write_text(text + "\n", encoding="utf-8")

    if errors:
        print("\nErrors:", file=sys.stderr)
        for e in errors:
            print(f"  {e}", file=sys.stderr)
        raise SystemExit(1)


if __name__ == "__main__":
    main()
