#!/usr/bin/env python3
"""Sync iOS .xcstrings translations into Compose Multiplatform string resources.

The script reads `Darwin/YABA/Localizable.xcstrings` and writes per-locale
`strings.xml` files under `Compose/YABA/composeApp/src/commonMain/composeResources`.
It overwrites existing files (adding new strings, updating changed ones, and
removing stale ones) so Compose can reuse the same translations as iOS.

How to run (copy/paste):
    # Default paths, write files
    python3 Scripts/xcstrings_to_compose.py

    # Preview without writing
    python3 Scripts/xcstrings_to_compose.py --dry-run --verbose

    # Custom paths and source language
    python3 Scripts/xcstrings_to_compose.py \
        --xcstrings /absolute/path/to/Localizable.xcstrings \
        --out /absolute/path/to/composeResources \
        --source-lang en \
        --verbose

Arguments:
    --xcstrings PATH   Path to Localizable.xcstrings. Default: Darwin/YABA/Localizable.xcstrings
    --out PATH         Output composeResources dir. Default: Compose/YABA/composeApp/src/commonMain/composeResources
    --source-lang CODE Override source language code (defaults to xcstrings sourceLanguage)
    --dry-run          Show planned changes without writing files
    --verbose          Print per-locale update details
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Dict, Iterable, Mapping, MutableMapping

# Repo-root-relative defaults
ROOT = Path(__file__).resolve().parent.parent
DEFAULT_XCSTRINGS = ROOT / "Darwin" / "YABA" / "Localizable.xcstrings"
DEFAULT_OUTPUT_ROOT = (
    ROOT / "Compose" / "YABA" / "composeApp" / "src" / "commonMain" / "composeResources"
)

# Patterns for transforming iOS format specifiers and legal Android resource names.
PLACEHOLDER_PATTERN = re.compile(r"(?<!%)%(?:(\d+)\$)?(@|lld|ld|d|f|s)")
INVALID_NAME_PATTERN = re.compile(r"[^a-z0-9_]+")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Convert Localizable.xcstrings into Compose Multiplatform strings.xml files "
            "per language."
        )
    )
    parser.add_argument(
        "--xcstrings",
        type=Path,
        default=DEFAULT_XCSTRINGS,
        help="Path to Localizable.xcstrings (default: repo Darwin/YABA/Localizable.xcstrings)",
    )
    parser.add_argument(
        "--out",
        type=Path,
        default=DEFAULT_OUTPUT_ROOT,
        help=(
            "Directory to place generated composeResources (default: "
            "Compose/YABA/composeApp/src/commonMain/composeResources)"
        ),
    )
    parser.add_argument(
        "--source-lang",
        dest="source_lang",
        help="Override source language (defaults to the xcstrings sourceLanguage field).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Show planned changes without writing files.",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="Print per-locale update details.",
    )
    return parser.parse_args()


def load_xcstrings(path: Path) -> Mapping:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise SystemExit(f"xcstrings file not found: {path}") from exc
    except json.JSONDecodeError as exc:
        raise SystemExit(f"xcstrings file is not valid JSON: {path}") from exc


def extract_string_value(localization_entry: Mapping) -> str | None:
    """Return the best-effort string value from a localization entry."""
    unit = (localization_entry or {}).get("stringUnit") or {}
    if not unit:
        return None
    if "value" in unit and unit["value"] is not None:
        return str(unit["value"])
    variations = unit.get("variations")
    if variations:
        return _first_variation_string(variations)
    return None


def _first_variation_string(variations: Mapping) -> str | None:
    """Depth-first search for the first value in a variations tree."""
    if not isinstance(variations, Mapping):
        return None
    for variant in variations.values():
        if isinstance(variant, Mapping):
            if "stringUnit" in variant:
                candidate = extract_string_value(variant)
                if candidate is not None:
                    return candidate
            candidate = _first_variation_string(variant)
            if candidate is not None:
                return candidate
    return None


def collect_locale_strings(strings_section: Mapping, source_language: str) -> Dict[str, Dict[str, str]]:
    """Flatten the xcstrings structure into {locale: {key: value}} with source fallbacks."""
    locale_strings: Dict[str, Dict[str, str]] = {}

    for key, entry in (strings_section or {}).items():
        localizations = entry.get("localizations") or {}
        for locale, localization_entry in localizations.items():
            value = extract_string_value(localization_entry)
            if value is None:
                continue
            locale_strings.setdefault(locale, {})[key] = value

    if not locale_strings:
        return {}

    # Add source-language fallbacks so every locale has a complete set.
    source_values = locale_strings.get(source_language, {})
    if source_values:
        for locale, values in locale_strings.items():
            if locale == source_language:
                continue
            for key, fallback in source_values.items():
                values.setdefault(key, fallback)
    else:
        # If the source language is missing, derive it from the first available translation per key.
        derived_source: Dict[str, str] = {}
        for key in {k for values in locale_strings.values() for k in values}:
            for values in locale_strings.values():
                if key in values:
                    derived_source[key] = values[key]
                    break
        if derived_source:
            locale_strings[source_language] = derived_source

    return locale_strings


def build_name_map(locale_strings: Mapping[str, Mapping[str, str]]) -> Dict[str, str]:
    """Create a deterministic map of original keys to Android-safe resource names."""
    used_names: Dict[str, str] = {}
    name_map: Dict[str, str] = {}
    all_keys = sorted({key for values in locale_strings.values() for key in values})
    for key in all_keys:
        name_map[key] = sanitize_resource_name(key, used_names)
    return name_map


def sanitize_resource_name(key: str, used: MutableMapping[str, str]) -> str:
    slug = INVALID_NAME_PATTERN.sub("_", key.strip().lower()).strip("_")
    if not slug:
        slug = "string"
    if slug[0].isdigit():
        slug = f"s_{slug}"

    name = slug
    if name in used and used[name] != key:
        suffix = hashlib.sha1(key.encode("utf-8")).hexdigest()[:8]
        name = f"{slug}_{suffix}"
    used[name] = key
    return name


def convert_placeholders(value: str) -> str:
    """Convert common iOS format tokens to Android-compatible ones."""

    def _repl(match: re.Match[str]) -> str:
        position, token = match.groups()
        mapped = {"@": "s", "lld": "d", "ld": "d", "d": "d", "f": "f", "s": "s"}[token]
        pos = f"{position}$" if position else ""
        return f"%{pos}{mapped}"

    return PLACEHOLDER_PATTERN.sub(_repl, value)


def escape_android_string(value: str) -> str:
    escaped = value.replace("\\", "\\\\")
    escaped = escaped.replace("\r\n", "\n").replace("\r", "\n")
    escaped = escaped.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    escaped = escaped.replace('"', '\\"')
    escaped = escaped.replace("\n", "\\n").replace("\t", "\\t")
    return escaped


def locale_to_dir(locale: str, source_language: str) -> str:
    """Return an Android/Compose-compatible values directory for a locale."""
    normalized = (locale or "").replace("_", "-")
    parts = [p for p in normalized.split("-") if p]
    if not parts:
        return "values"

    lang = parts[0].lower()
    qualifiers = [f"r{part}" for part in parts[1:]]
    if lang == (source_language or "").lower() and not qualifiers:
        return "values"
    suffix = "-".join([lang] + qualifiers) if qualifiers else lang
    return f"values-{suffix}"


def write_strings_file(root: Path, locale_dir: str, values: Mapping[str, str], dry_run: bool) -> bool:
    output_dir = root / locale_dir
    output_file = output_dir / "strings.xml"
    output_dir.mkdir(parents=True, exist_ok=True)

    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for name in sorted(values):
        lines.append(f'    <string name="{name}">{values[name]}</string>')
    lines.append("</resources>")
    content = "\n".join(lines) + "\n"

    if dry_run:
        return True

    existing = output_file.read_text(encoding="utf-8") if output_file.exists() else None
    if existing == content:
        return False

    output_file.write_text(content, encoding="utf-8")
    return True


def clean_stale_strings_files(root: Path, expected_locale_dirs: Iterable[str], dry_run: bool) -> list[Path]:
    """Remove strings.xml files for locales no longer present in xcstrings."""
    expected = set(expected_locale_dirs)
    removed: list[Path] = []
    for path in root.glob("values*/strings.xml"):
        if path.parent.name in expected:
            continue
        removed.append(path)
        if not dry_run:
            try:
                path.unlink()
                path.parent.rmdir()
            except FileNotFoundError:
                pass
            except OSError:
                # Directory not empty; leave it alone.
                pass
    return removed


def main() -> int:
    args = parse_args()
    data = load_xcstrings(args.xcstrings)
    source_language = (args.source_lang or data.get("sourceLanguage") or "en").strip()

    locale_strings = collect_locale_strings(data.get("strings", {}), source_language)
    if not locale_strings:
        print("No strings found in xcstrings input.", file=sys.stderr)
        return 1

    name_map = build_name_map(locale_strings)
    output_root: Path = args.out
    output_root.mkdir(parents=True, exist_ok=True)

    locale_dirs: list[str] = []
    for locale in sorted(locale_strings):
        key_values = locale_strings[locale]
        named_values = {
            name_map[key]: escape_android_string(convert_placeholders(value))
            for key, value in key_values.items()
            if key in name_map
        }
        locale_dir = locale_to_dir(locale, source_language)
        locale_dirs.append(locale_dir)
        changed = write_strings_file(output_root, locale_dir, named_values, args.dry_run)
        if args.verbose or args.dry_run:
            action = "Would write" if args.dry_run else ("Updated" if changed else "Unchanged")
            print(f"{action}: {locale_dir}/strings.xml ({len(named_values)} strings)")

    removed = clean_stale_strings_files(output_root, locale_dirs, args.dry_run)
    if args.verbose or args.dry_run:
        for path in removed:
            action = "Would remove" if args.dry_run else "Removed"
            print(f"{action}: {path.relative_to(output_root)}")

    if not args.verbose and not args.dry_run:
        print(f"Generated {len(locale_dirs)} locale file(s) in {output_root}")

    return 0


if __name__ == "__main__":
    sys.exit(main())

