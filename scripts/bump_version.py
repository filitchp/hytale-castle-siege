#!/usr/bin/env python3
import argparse
import json
import re
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
MANIFEST_PATH = PROJECT_ROOT / "src" / "main" / "resources" / "manifest.json"
WORLD_CONFIG_PATH = (
    PROJECT_ROOT / "devserver" / "universe" / "worlds" / "default" / "config.json"
)

VERSION_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")
PARTS = ("major", "minor", "patch")


def next_version(current: str, target: str) -> str:
    if VERSION_RE.match(target):
        return target
    match = VERSION_RE.match(current)
    if not match:
        raise ValueError(f"Current version {current!r} is not MAJOR.MINOR.PATCH")
    major, minor, patch = (int(p) for p in match.groups())
    if target == "major":
        return f"{major + 1}.0.0"
    if target == "minor":
        return f"{major}.{minor + 1}.0"
    return f"{major}.{minor}.{patch + 1}"


# Rewrite a single field in place so the rest of each file keeps its exact
# formatting (the world config is written by the server and has float/binary
# fields that a json round-trip would reformat).
def replace_field(text: str, field: str, new_value: str, path: Path) -> str:
    pattern = re.compile(rf'("{field}"\s*:\s*)"[^"]*"')
    updated, count = pattern.subn(
        lambda m: m.group(1) + json.dumps(new_value), text, count=1
    )
    if count != 1:
        raise ValueError(f"No {field!r} field found in {path}")
    return updated


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Bump the Castle Siege release version in the mod manifest "
        "and the dev world's DisplayName."
    )
    parser.add_argument(
        "target",
        nargs="?",
        default="patch",
        help="major, minor, patch (default), or an explicit version like 1.2.0",
    )
    parser.add_argument(
        "--dry-run",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Show the change without writing files (default); "
        "pass --no-dry-run to write them",
    )
    args = parser.parse_args()

    if args.target not in PARTS and not VERSION_RE.match(args.target):
        parser.error(f"target must be one of {', '.join(PARTS)} or MAJOR.MINOR.PATCH")

    manifest_text = MANIFEST_PATH.read_text()
    current = json.loads(manifest_text)["Version"]
    new = next_version(current, args.target)

    world_text = WORLD_CONFIG_PATH.read_text()
    display_name = json.loads(world_text)["DisplayName"]
    new_display_name, count = re.subn(r"v\d+\.\d+\.\d+", f"v{new}", display_name)
    if count != 1:
        print(
            f"DisplayName {display_name!r} in {WORLD_CONFIG_PATH} has no vX.Y.Z version",
            file=sys.stderr,
        )
        return 1

    new_manifest_text = replace_field(manifest_text, "Version", new, MANIFEST_PATH)
    new_world_text = replace_field(
        world_text, "DisplayName", new_display_name, WORLD_CONFIG_PATH
    )

    print(f"Version:     {current} -> {new}")
    print(f"DisplayName: {display_name} -> {new_display_name}")
    if new_manifest_text == manifest_text and new_world_text == world_text:
        print("Already up to date")
        return 0
    if args.dry_run:
        print("Dry run, no files written. Re-run with --no-dry-run to apply.")
        return 0

    MANIFEST_PATH.write_text(new_manifest_text)
    WORLD_CONFIG_PATH.write_text(new_world_text)
    print(f"Updated {MANIFEST_PATH.relative_to(PROJECT_ROOT)}")
    print(f"Updated {WORLD_CONFIG_PATH.relative_to(PROJECT_ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
