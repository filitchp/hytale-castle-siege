#!/usr/bin/env python3
import json
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image

PROJECT_ROOT = Path(__file__).resolve().parent.parent
MANIFEST_PATH = PROJECT_ROOT / "src" / "main" / "resources" / "manifest.json"


def main() -> int:
    original_text = MANIFEST_PATH.read_text()
    manifest = json.loads(original_text)
    version = manifest["Version"]

    patched = dict(manifest)
    patched["IncludesAssetPack"] = True
    MANIFEST_PATH.write_text(json.dumps(patched, indent=4) + "\n")

    try:
        result = subprocess.run(
            ["./gradlew", "build"],
            cwd=PROJECT_ROOT,
        )
    finally:
        MANIFEST_PATH.write_text(original_text)

    if result.returncode != 0:
        return result.returncode

    release_dir = PROJECT_ROOT / "release" / f"v{version}"
    world_dir = release_dir / f"Castle-Siege-World-v{version}"
    world_dir.mkdir(parents=True, exist_ok=True)

    shutil.copy2(PROJECT_ROOT / "scripts" / "config.json", world_dir / "config.json")
    shutil.copytree(
        PROJECT_ROOT / "devserver" / "universe",
        world_dir / "universe",
        dirs_exist_ok=True,
    )
    shutil.rmtree(world_dir / "universe" / "players", ignore_errors=True)

    mods_dir = world_dir / "mods"
    mods_dir.mkdir(exist_ok=True)
    shutil.copy2(
        PROJECT_ROOT / "build" / "libs" / "dev.dooondi.castlesiege.jar",
        mods_dir / "dev.dooondi.castlesiege.jar",
    )

    target_w, target_h = 576, 360
    with Image.open(PROJECT_ROOT / "media" / "icon.png") as icon:
        scaled_h = round(icon.height * target_w / icon.width)
        scaled = icon.resize((target_w, scaled_h), Image.LANCZOS)
        crop_top = (scaled_h - target_h) // 2 - 40
        scaled.crop((0, crop_top, target_w, crop_top + target_h)).save(
            world_dir / "preview.png"
        )

    print(f"Release prepared at {release_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
