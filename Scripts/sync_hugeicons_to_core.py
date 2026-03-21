#!/usr/bin/env python3
"""Copy HugeIcons category folders into Core composeResources and regenerate metadata.

Layout expected:
    HugeIcons/<category_slug>/<icon>.svg

Skips the `islamic` category folder. Never deletes existing SVGs in Core (only adds/overwrites
from HugeIcons). Warns if Core already contains an icon not found under HugeIcons.

Usage:
    python3 Scripts/sync_hugeicons_to_core.py --dry-run --verbose
    python3 Scripts/sync_hugeicons_to_core.py --verbose
"""

from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Mapping, MutableMapping, Sequence, Tuple

ROOT = Path(__file__).resolve().parent.parent

DEFAULT_HUGEICONS = ROOT / "HugeIcons"
DEFAULT_CORE_ICONS = (
    ROOT
    / "Core"
    / "YABACore"
    / "src"
    / "commonMain"
    / "composeResources"
    / "files"
    / "icons"
)
DEFAULT_CORE_METADATA = (
    ROOT
    / "Core"
    / "YABACore"
    / "src"
    / "commonMain"
    / "composeResources"
    / "files"
    / "metadata"
)

METADATA_VERSION = "3.0"
HEADER_DESCRIPTION = "HugeIcons stroke-rounded categories (generated; TODO: localize)"

# YabaColor codes 1..13 (see YabaColor.kt); cycle for categories
COLOR_CODES: Tuple[int, ...] = tuple(range(1, 14))

# Never remove these from metadata dir
METADATA_PRESERVE = frozenset({"preload_data.json", "icon_categories_header.json"})


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--hugeicons", type=Path, default=DEFAULT_HUGEICONS, help="HugeIcons root")
    p.add_argument("--core-icons", type=Path, dest="core_icons", default=DEFAULT_CORE_ICONS)
    p.add_argument("--core-metadata", type=Path, dest="core_metadata", default=DEFAULT_CORE_METADATA)
    p.add_argument("--dry-run", action="store_true", help="Print actions only; do not write files")
    p.add_argument("--verbose", action="store_true")
    return p.parse_args()


def slugify_folder_name(name: str) -> str:
    """Folder name is already a slug (e.g. add_remove). Normalize and skip islamic."""
    s = name.strip().lower().replace(" ", "_")
    return s


def humanize_slug(slug: str) -> str:
    """Display name: add_remove -> Add Remove (TODO: real localization)."""
    parts = slug.replace("-", "_").split("_")
    return " ".join(p.capitalize() for p in parts if p)


def collect_hugeicons(hugeicons: Path) -> Tuple[Dict[str, List[str]], List[str]]:
    """Returns (category_slug -> sorted icon stems, duplicate_warnings)."""
    if not hugeicons.is_dir():
        raise SystemExit(f"Not a directory: {hugeicons}")

    categories: Dict[str, List[str]] = {}
    seen_globally: MutableMapping[str, str] = {}
    dup_warnings: List[str] = []

    for child in sorted(hugeicons.iterdir()):
        if not child.is_dir():
            continue
        slug = slugify_folder_name(child.name)
        if slug == "islamic":
            continue
        svg_names: List[str] = []
        for f in sorted(child.glob("*.svg")):
            stem = f.stem
            svg_names.append(stem)
            prev = seen_globally.get(stem)
            if prev is not None and prev != slug:
                dup_warnings.append(
                    f"Duplicate icon name '{stem}' in categories '{prev}' and '{slug}' (same file in Core)"
                )
            else:
                seen_globally[stem] = slug
        categories[slug] = svg_names

    return categories, dup_warnings


def collect_core_icon_stems(core_icons: Path) -> List[str]:
    if not core_icons.is_dir():
        return []
    return sorted(p.stem for p in core_icons.glob("*.svg"))


def copy_svgs(
    hugeicons: Path,
    categories: Mapping[str, Sequence[str]],
    core_icons: Path,
    dry_run: bool,
    verbose: bool,
) -> Tuple[int, int]:
    """Copy SVGs from HugeIcons into core_icons. Returns (copied, skipped_same)."""
    copied = 0
    skipped = 0
    # Deterministic: process categories alphabetically, then icons — first category wins on duplicate name
    for slug in sorted(categories):
        cat_dir = hugeicons / slug
        if not cat_dir.is_dir():
            # folder might use different casing; try original scan path
            matches = [d for d in hugeicons.iterdir() if d.is_dir() and slugify_folder_name(d.name) == slug]
            cat_dir = matches[0] if matches else hugeicons / slug
        for stem in categories[slug]:
            src = cat_dir / f"{stem}.svg"
            if not src.is_file():
                print(f"WARN: missing file {src}", file=sys.stderr)
                continue
            dst = core_icons / f"{stem}.svg"
            if dst.is_file():
                try:
                    if dst.read_bytes() == src.read_bytes():
                        skipped += 1
                        if verbose:
                            print(f"  skip (identical): {dst.name}")
                        continue
                except OSError as e:
                    print(f"WARN: read compare failed {dst}: {e}", file=sys.stderr)
            if dry_run:
                print(f"  would copy: {src} -> {dst}")
                copied += 1
            else:
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(src, dst)
                copied += 1
                if verbose:
                    print(f"  copied: {dst.name}")
    return copied, skipped


def build_header_and_files(
    categories: Mapping[str, Sequence[str]],
) -> Tuple[dict, Dict[str, dict]]:
    """Build icon_categories_header payload and per-file JSON dicts."""
    slugs = sorted(categories.keys())
    total_icons = sum(len(v) for v in categories.values())

    header_categories: List[dict] = []
    per_file: Dict[str, dict] = {}

    for idx, slug in enumerate(slugs):
        icons = sorted(categories[slug])
        icon_count = len(icons)
        color = COLOR_CODES[idx % len(COLOR_CODES)]
        display = humanize_slug(slug)
        header_icon = icons[0] if icons else "add-01"
        filename = f"{slug}.json"

        sub = {
            "id": slug,
            "name": display,
            "description": f"{display} icons",  # TODO(localization): replace with string resources
            "header_icon": header_icon,
            "color": color,
            "icon_count": icon_count,
            "filename": filename,
        }
        cat = {
            "id": slug,
            "name": display,
            "description": f"Browse {display} icons",  # TODO(localization)
            "icon_count": icon_count,
            "filename": filename,
            "header_icon": header_icon,
            "color": color,
            "subcategories": [sub],
        }
        header_categories.append(cat)

        per_file[filename] = {
            "metadata": {
                "id": slug,
                "name": display,
                "description": f"{display} stroke-rounded icons from HugeIcons",
                "main_category": slug,
                "icon_count": icon_count,
                "version": METADATA_VERSION,
            },
            "icons": [{"name": n} for n in icons],
        }

    header = {
        "metadata": {
            "total_categories": len(slugs),
            "total_subcategories": len(slugs),
            "total_icons": total_icons,
            "version": METADATA_VERSION,
            "description": HEADER_DESCRIPTION,
        },
        "categories": header_categories,
    }
    return header, per_file


def write_json(path: Path, data: object, dry_run: bool) -> bool:
    text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
    if dry_run:
        print(f"  would write: {path} ({len(text)} bytes)")
        return True
    path.parent.mkdir(parents=True, exist_ok=True)
    old = path.read_text(encoding="utf-8") if path.exists() else None
    if old == text:
        return False
    path.write_text(text, encoding="utf-8")
    return True


def remove_stale_metadata(
    metadata_dir: Path,
    keep_filenames: Iterable[str],
    dry_run: bool,
    verbose: bool,
) -> List[Path]:
    keep = set(keep_filenames) | METADATA_PRESERVE
    removed: List[Path] = []
    for p in sorted(metadata_dir.glob("*.json")):
        if p.name in keep:
            continue
        removed.append(p)
        if dry_run:
            print(f"  would remove stale: {p.name}")
        else:
            p.unlink()
            if verbose:
                print(f"  removed stale: {p.name}")
    return removed


def main() -> int:
    args = parse_args()
    hugeicons: Path = args.hugeicons
    core_icons: Path = args.core_icons
    core_metadata: Path = args.core_metadata

    categories, dup_warnings = collect_hugeicons(hugeicons)
    for w in dup_warnings:
        print(f"WARN: {w}", file=sys.stderr)

    if not categories:
        print("No categories found under HugeIcons (after skipping islamic).", file=sys.stderr)
        return 1

    # Core icons missing from HugeIcons index
    huge_stems = {s for names in categories.values() for s in names}
    for stem in collect_core_icon_stems(core_icons):
        if stem not in huge_stems:
            print(
                f"WARN: Core icon '{stem}.svg' not found in HugeIcons — keeping file; not in regenerated metadata lists",
                file=sys.stderr,
            )

    copied, skipped = copy_svgs(hugeicons, categories, core_icons, args.dry_run, args.verbose)
    header, per_file = build_header_and_files(categories)

    keep_names = list(per_file.keys())
    remove_stale_metadata(core_metadata, keep_names, args.dry_run, args.verbose)

    header_path = core_metadata / "icon_categories_header.json"
    if args.verbose or args.dry_run:
        print(f"{'Would write' if args.dry_run else 'Writing'} header: {header_path}")
    write_json(header_path, header, args.dry_run)

    for fn, data in sorted(per_file.items()):
        write_json(core_metadata / fn, data, args.dry_run)

    print(
        f"Done: categories={len(categories)} svg_copy={'dry-run' if args.dry_run else copied} "
        f"unchanged={skipped} metadata_files={len(per_file)}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
