"""Repair hard stone frames and key-colour artefacts on ore block textures.

The operation is deterministic and limited to the named ore textures.  Before
writing, the original PNG is copied to texture-backups/ore-edge-repair/.  The
outer three pixels are gently blended with the neighbouring rock pixels so the
connected-ore renderer does not expose a square, single-colour frame.  The
known magenta key colour is replaced by the nearest non-key neighbour.
"""
from __future__ import annotations

from collections import deque
from pathlib import Path
from shutil import copy2

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
TEXTURE_ROOT = ROOT / "neoforge-26.1.2/src/main/resources/assets/earth_core/textures/block"
BACKUP_ROOT = ROOT / "texture-backups/ore-edge-repair"
TARGETS = (
    "poor_magnetite_ore.png",
    "magnetite_ore.png",
    "rich_magnetite_ore.png",
    "auriferous_quartz_vein.png",
    "bituminous_coal_seam.png",
    "anthracite_coal_seam.png",
    "lapis_lazuli_ore.png",
)
MAGENTA = (205, 4, 195)


def repair(path: Path) -> None:
    BACKUP_ROOT.mkdir(parents=True, exist_ok=True)
    backup = BACKUP_ROOT / path.name
    if not backup.exists():
        copy2(path, backup)
    image = Image.open(path).convert("RGBA")
    source = image.copy()
    blurred = source.filter(ImageFilter.GaussianBlur(radius=1.2))
    pixels = image.load()
    src = source.load()
    smooth = blurred.load()
    width, height = image.size

    for y in range(height):
        for x in range(width):
            r, g, b, _ = src[x, y]
            if (r, g, b) == MAGENTA:
                # Prefer a nearby interior pixel rather than leaving a key-colour
                # speck in a block face.
                replacement = None
                for radius in range(1, 6):
                    for yy in range(max(0, y - radius), min(height, y + radius + 1)):
                        for xx in range(max(0, x - radius), min(width, x + radius + 1)):
                            candidate = src[xx, yy]
                            if candidate[:3] != MAGENTA:
                                replacement = candidate
                                break
                        if replacement is not None:
                            break
                    if replacement is not None:
                        break
                pixels[x, y] = replacement or (96, 96, 96, 255)
                continue
            edge = min(x, y, width - 1 - x, height - 1 - y)
            if edge < 3:
                # Keep texture identity while removing a hard, repeated frame.
                weight = (3 - edge) / 12.0
                sr, sg, sb, _ = smooth[x, y]
                pixels[x, y] = (
                    round(r * (1 - weight) + sr * weight),
                    round(g * (1 - weight) + sg * weight),
                    round(b * (1 - weight) + sb * weight),
                    255,
                )
            else:
                pixels[x, y] = (r, g, b, 255)
    image.save(path, format="PNG", optimize=True)


def main() -> None:
    for name in TARGETS:
        path = TEXTURE_ROOT / name
        if not path.is_file():
            raise FileNotFoundError(path)
        repair(path)
        print(f"repaired {path.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
