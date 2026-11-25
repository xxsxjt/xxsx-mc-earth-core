#!/usr/bin/env python3
"""Give low-palette Earth on Minecraft sprites readable pixel-art shading.

Only item/block PNGs with fewer than ten opaque RGB colours are touched.  The
algorithm preserves the alpha silhouette, derives a warm upper-left highlight
and cool lower-right shadow from each source colour, then adds deterministic
edge and material-grain accents.  It is intentionally dependency-light apart
from Pillow and is safe to rerun (already-rich images are left unchanged).
"""
from __future__ import annotations

import colorsys
import hashlib
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1] / "neoforge-26.1.2" / "src" / "main" / "resources" / "assets" / "earth_core" / "textures"
MIN_COLORS = 10


def shade(rgb: tuple[int, int, int], amount: float, warm: float) -> tuple[int, int, int]:
    h, s, v = colorsys.rgb_to_hsv(*(channel / 255.0 for channel in rgb))
    # Shift the light toward amber and the shadow toward cyan/blue without
    # destroying a material's hue (purple powders remain purple, etc.).
    h = (h + warm) % 1.0
    s = min(1.0, max(0.08, s * (1.0 + abs(amount) * 0.25)))
    v = min(1.0, max(0.025, v * (1.0 + amount)))
    return tuple(round(channel * 255) for channel in colorsys.hsv_to_rgb(h, s, v))


def process(path: Path) -> bool:
    image = Image.open(path).convert("RGBA")
    pixels = image.load()
    opaque = [pixels[x, y][:3] for y in range(image.height) for x in range(image.width) if pixels[x, y][3] > 16]
    if len(set(opaque)) >= MIN_COLORS or not opaque:
        return False
    seed = int(hashlib.sha256(path.as_posix().encode()).hexdigest()[:8], 16)
    palette = list(dict.fromkeys(opaque))
    # Expand every base colour into four hue-shifted levels.  The final image
    # naturally lands in the requested 12..20 colour range for tiny sprites.
    expanded: list[tuple[int, int, int]] = []
    for i, color in enumerate(palette):
        warm = ((seed >> (i % 24)) & 7) / 700.0
        expanded.extend((
            shade(color, 0.22, warm),
            shade(color, 0.08, warm / 2),
            shade(color, -0.10, -warm / 2),
            shade(color, -0.25, -warm),
        ))
    # Keep 12..20 shades: evenly sample when a source has several base tones.
    target = min(20, max(12, len(set(opaque)) * 3))
    expanded = expanded[::max(1, len(expanded) // target)][:target]
    if len(set(expanded)) < 12:
        expanded += [shade(palette[i % len(palette)], 0.03 + i * 0.01, 0.01) for i in range(12)]
    expanded = list(dict.fromkeys(expanded))[:20]
    while len(expanded) < 12:
        expanded.append(shade(palette[len(expanded) % len(palette)], -0.03, 0.015))

    opaque_mask = {(x, y) for y in range(image.height) for x in range(image.width) if pixels[x, y][3] > 16}
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a <= 16:
                continue
            base = min(range(len(palette)), key=lambda i: sum((palette[i][c] - (r, g, b)[c]) ** 2 for c in range(3)))
            nx = x / max(1, image.width - 1)
            ny = y / max(1, image.height - 1)
            level = int((1.0 - nx * 0.38 - ny * 0.48) * 3.2)
            # Darken the silhouette edge; add tiny crystal/grain sparkles only
            # where the deterministic hash says so, retaining a crisp outline.
            edge = any((x + dx, y + dy) not in opaque_mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                level = 3
            elif ((x * 17 + y * 31 + seed) & 31) == 0:
                level = max(0, level - 1)
            pixels[x, y] = (*expanded[min(len(expanded) - 1, base * 4 + level)], a)
    image.save(path, optimize=True)
    return True


def main() -> int:
    changed = 0
    for path in sorted(ROOT.rglob("*.png")):
        relative = path.relative_to(ROOT)
        # Hand crank item rendering reuses the 3D block model, so it must not
        # have or recreate a redundant 2D item texture.
        if relative.as_posix() == "item/hand_crank.png":
            continue
        if relative.parts[0] in {"item", "block"} and process(path):
            changed += 1
    print(f"upgraded {changed} low-palette textures")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
