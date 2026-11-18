#!/usr/bin/env python3
"""Render an offline, pixel-aligned audit of four processing-machine GUIs."""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT.parent.parent / "workflow-reports" / "eom-gui-preview-20261108.png"
W, H, SCALE = 176, 188, 2
CORE = (66, 31, 42, 34)
GAUGE_Y, GAUGE_H = 24, 52
PALETTES = {
    "COMMINUTION": ("#211B17", "#3B322B", "#181512", "#B08A57", "#6E5B48", "#E2E8EC"),
    "THERMAL": ("#2B1713", "#4B2B23", "#211311", "#E19A45", "#86513A", "#FFE7CF"),
    "REACTION": ("#2B2114", "#4B3A23", "#211A11", "#E2AE59", "#896B3C", "#FFECD0"),
    "ELECTROCHEMICAL": ("#111D30", "#203854", "#0D1828", "#7EB4DE", "#8B5D52", "#E2E8EC"),
}


@dataclass(frozen=True)
class Profile:
    name: str
    family: str
    secondary: int
    reagent: bool
    fuel: bool
    outputs: int
    fluid: bool
    temperature: bool
    energy: bool
    fuel_name: str = ""


PANELS = (
    Profile("CRUSHER", "COMMINUTION", 0, False, True, 6, False, False, True, "DRIVE"),
    Profile("REDUCTION_FURNACE", "THERMAL", 1, True, True, 4, False, True, True, "FUEL"),
    Profile("CHEMICAL_REACTOR", "REACTION", 2, True, False, 4, True, False, True),
    Profile("ELECTROLYTIC_CELL", "ELECTROCHEMICAL", 0, False, False, 4, False, True, True),
)


def outputs(count: int) -> tuple[tuple[int, int], ...]:
    if count <= 2:
        return ((116, 44), (142, 44), (116, 24), (142, 24), (116, 64), (142, 64), (142, 82))
    if count <= 4:
        return ((116, 33), (140, 33), (116, 57), (140, 57), (116, 75), (140, 75), (158, 75))
    return ((112, 33), (132, 33), (152, 33), (112, 55), (132, 55), (152, 55), (152, 75))


def machine_slots(p: Profile) -> list[tuple[str, int, int, str]]:
    slots = [("input", 28 if p.secondary else 38, 30 if p.secondary else (30 if p.fuel else 44), "ORE")]
    if p.fuel:
        fy = 58 if p.secondary == 1 else (70 if p.secondary > 1 else 62)
        slots.append(("fuel", 28 if p.secondary else 38, fy, p.fuel_name))
    secondary = ((48, 30), (28, 50), (48, 50), (48, 70))
    for i in range(p.secondary):
        slots.append(("secondary", *secondary[i], f"AUX {i + 1}"))
    for x, y in outputs(p.outputs)[:p.outputs]:
        slots.append(("output", x, y, "OUT"))
    if p.reagent:
        slots.append(("reagent", 80, 66, "COKE" if p.name == "REDUCTION_FURNACE" else "CAT"))
    return slots


def gauges(p: Profile) -> list[tuple[str, int, int, int, int]]:
    kinds = (["energy"] if p.energy else []) + (["temperature"] if p.temperature else []) + (["fluid"] if p.fluid else [])
    result = []
    for i, kind in enumerate(kinds):
        x = 8 + i * 7 if len(kinds) >= 3 else 10 + i * 8
        result.append((kind, x - 1, GAUGE_Y - 1, 8, GAUGE_H + 2))
    return result


def intersects(a: tuple[int, int, int, int, int], b: tuple[int, int, int, int, int]) -> bool:
    ax, ay, aw, ah = a[1:]
    bx, by, bw, bh = b[1:]
    return ax < bx + bw and bx < ax + aw and ay < by + bh and by < ay + ah


def validate(p: Profile) -> None:
    """All collision rectangles use the actual Java/PIL half-open pixel bounds."""
    obstacles = [("core chamber", *CORE)]
    obstacles += gauges(p)
    errors = []
    rects = [(name, x, y, 18, 18) for name, x, y, _ in machine_slots(p)]
    for i, a in enumerate(rects):
        for b in rects[i + 1:]:
            if intersects(a, b):
                errors.append(f"{p.name}: {a} overlaps {b}")
        for obstacle in obstacles:
            if intersects(a, obstacle):
                errors.append(f"{p.name}: {a} overlaps {obstacle}")
    if p.fuel_name == "FUEL":
        fuel_y = 58 if p.secondary == 1 else 70
        flame = ("fuel flame", 46, 47, 2, 10) if p.secondary == 1 else ("fuel flame", 29, 71, 14, 14)
        for slot in rects:
            if intersects(flame, slot):
                errors.append(f"{p.name}: {flame} overlaps {slot}")
    if errors:
        raise RuntimeError("GUI collision validation failed:\n" + "\n".join(errors))
    out = outputs(p.outputs)[:p.outputs]
    for i, a in enumerate(out):
        for b in out[i + 1:]:
            assert math.dist(a, b) >= 18, f"{p.name}: outputs closer than 18px: {a}, {b}"


class Painter:
    def __init__(self, image: Image.Image, origin: tuple[int, int], p: Profile):
        self.d = ImageDraw.Draw(image)
        self.ox, self.oy = origin
        self.p = p
        self.palette = PALETTES[p.family]
        self.font = ImageFont.load_default(size=10 * SCALE)
        self.small = ImageFont.load_default(size=7 * SCALE)

    def rect(self, box: tuple[int, int, int, int], color: str) -> None:
        x0, y0, x1, y1 = box
        if x1 <= x0 or y1 <= y0:
            return
        # Java fill's end coordinates are exclusive; PIL's are inclusive.
        self.d.rectangle((self.ox + x0 * SCALE, self.oy + y0 * SCALE,
                          self.ox + x1 * SCALE - 1, self.oy + y1 * SCALE - 1), fill=color)

    def text(self, xy: tuple[int, int], text: str, color: str, font=None) -> None:
        self.d.text((self.ox + xy[0] * SCALE, self.oy + xy[1] * SCALE), text,
                    fill=color, font=font or self.font)

    def slot(self, kind: str, x: int, y: int) -> None:
        outer, main, work, accent, border, light = self.palette
        self.rect((x - 1, y - 1, x + 17, y + 17), outer)
        self.rect((x, y, x + 16, y + 16), border)
        self.rect((x + 1, y + 1, x + 15, y + 15), "#172025")
        self.rect((x + 1, y + 1, x + 15, y + 2), "#29343A")
        ghost = "#75644E" if self.p.family == "COMMINUTION" else "#746C60"
        if kind == "input":
            self.rect((x + 3, y + 3, x + 13, y + 5), ghost)
            self.rect((x + 4, y + 5, x + 6, y + 9), ghost)
            self.rect((x + 10, y + 5, x + 12, y + 9), ghost)
            self.rect((x + 6, y + 9, x + 10, y + 12), ghost)
            self.rect((x + 6, y + 12, x + 7, y + 14), ghost)
            self.rect((x + 9, y + 12, x + 10, y + 14), ghost)
        elif kind == "secondary":
            self.rect((x + 7, y + 3, x + 9, y + 13), ghost)
            self.rect((x + 3, y + 7, x + 13, y + 9), ghost)
            self.rect((x + 10, y + 10, x + 13, y + 13), ghost)
            self.rect((x + 11, y + 9, x + 12, y + 14), ghost)
        elif kind == "reagent":
            self.rect((x + 6, y + 2, x + 10, y + 4), ghost)
            self.rect((x + 7, y + 4, x + 9, y + 8), ghost)
            self.rect((x + 4, y + 10, x + 12, y + 14), ghost)
            self.rect((x + 5, y + 8, x + 6, y + 12), ghost)
            self.rect((x + 10, y + 8, x + 11, y + 12), ghost)
            self.rect((x + 5, y + 10, x + 11, y + 11), ghost)
        elif kind == "output":
            for dx, dy, w, h in ((3, 3, 3, 1), (3, 3, 1, 3), (10, 3, 3, 1), (12, 3, 1, 3),
                                  (3, 12, 3, 1), (3, 10, 1, 3), (10, 12, 3, 1), (12, 10, 1, 3), (7, 7, 2, 2)):
                self.rect((x + dx, y + dy, x + dx + w, y + dy + h), ghost)
        elif kind == "fuel":
            if self.p.fuel_name == "DRIVE":
                self.rect((x + 6, y + 3, x + 10, y + 13), "#526068")
                self.rect((x + 3, y + 6, x + 13, y + 10), "#526068")
                self.rect((x + 7, y + 7, x + 9, y + 9), "#172025")
            else:
                self.rect((x + 6, y + 4, x + 10, y + 12), "#654333")
                self.rect((x + 4, y + 8, x + 12, y + 13), "#654333")
                self.rect((x + 7, y + 2, x + 9, y + 7), "#8B5938")
        if kind == "output" and (x, y) == outputs(self.p.outputs)[0]:
            self.rect((x - 2, y - 2, x + 18, y), accent)
            self.rect((x - 2, y + 16, x + 18, y + 18), accent)
            self.rect((x - 2, y, x, y + 16), accent)
            self.rect((x + 16, y, x + 18, y + 16), accent)
            for dx, dy in ((-2, -2), (16, -2), (-2, 16), (16, 16)):
                self.rect((x + dx, y + dy, x + dx + 2, y + dy + 2), light)

    def panel(self) -> None:
        p = self.p
        outer, main, work, accent, border, light = self.palette
        self.rect((0, 0, W, H), outer)
        self.rect((2, 2, 174, 186), border)
        self.rect((4, 4, 172, 184), main)
        self.rect((6, 20, 170, 90), work)
        self.rect((6, 2, 170, 18), main)
        self.rect((7, 19, 169, 20), accent)
        self.rect((7, 91, 169, 92), border)
        self.rect((7, 99, 169, 100), accent)
        self.rect((7, 102, 169, 181), main)
        self.rect((CORE[0], CORE[1], CORE[0] + CORE[2], CORE[1] + CORE[3]), outer)
        self.rect((67, 32, 107, 64), border)
        self.rect((68, 33, 106, 63), "#0B1114")
        left = 65 if p.secondary else 55
        right = 110 if p.outputs > 4 else 114
        self.rect((left, 46, 66, 50), outer)
        self.rect((left + 1, 47, 66, 49), border)
        self.rect((108, 46, right, 50), outer)
        self.rect((108, 47, right - 1, 49), border)
        for kind, x, y, _ in machine_slots(p):
            self.slot(kind, x, y)
        for i, (kind, x, y, w, h) in enumerate(gauges(p)):
            self.rect((x, y, x + w, y + h), outer)
            self.rect((x + 1, y + 1, x + w - 1, y + h - 1), border)
            self.rect((x + 2, y + 2, x + w - 2, y + h - 2), "#0A1115")
            color = {"energy": "#39B7D4", "temperature": "#E86A24", "fluid": "#3E8FD0"}[kind]
            self.rect((x + 2, y + 27, x + w - 2, y + h - 2), color)
            if kind == "fluid":
                self.rect((x + 4, y + 2, x + 5, y + h - 2), "#27424F")
                self.rect((x + 5, y + 37, x + w - 2, y + h - 2), "#D9822B")
        self.motif()
        self.text((27, 6), p.name.replace("_", " "), light, self.small)
        self.rect((8, 3, 23, 18), border)
        self.rect((10, 5, 21, 16), accent)
        self.rect((112, 4, 170, 16), "#0B1114")
        self.rect((114, 6, 168, 14), "#11191D")
        self.rect((116, 8, 121, 13), "#8AF0A5")
        self.text((125, 6), "READY", light, self.small)
        self.text((8, 93), "INVENTORY", "#B8C4CC", self.small)
        for row in range(3):
            for col in range(9):
                self.slot("inventory", 8 + col * 18, 104 + row * 18)
        for col in range(9):
            self.slot("inventory", 8 + col * 18, 162)
        for y in (12, 34, 56, 78):
            self.tab(y)
        if p.fuel_name == "FUEL":
            # A narrow lit indicator occupies only x=46..48 between slot borders.
            self.rect((46, 47, 48, 48), "#FFE082")
            self.rect((46, 48, 48, 57), "#FF7A2E")
            self.rect((47, 48, 48, 56), "#FFC45B")

    def tab(self, y: int) -> None:
        outer, main, work, accent, border, light = self.palette
        self.rect((174, y + 3, 178, y + 17), border)
        self.rect((176, y, 196, y + 20), outer)
        self.rect((177, y + 1, 195, y + 19), border)
        self.rect((178, y + 2, 194, y + 18), main)
        self.rect((194, y + 3, 195, y + 17), accent)
        self.rect((181, y + 5, 190, y + 14), light)

    def motif(self) -> None:
        x, y, accent = 68, 33, self.palette[3]
        if self.p.family == "COMMINUTION":
            for box in ((2, 4, 12, 7), (5, 7, 12, 11), (8, 11, 12, 16), (26, 4, 36, 7), (26, 7, 33, 11), (26, 11, 30, 16)):
                self.rect((x + box[0], y + box[1], x + box[2], y + box[3]), "#7D6A56")
            for i in range(5):
                self.rect((x + 17 + (i % 2) * 3, y + 6 + i * 3, x + 19 + (i % 2) * 3, y + 8 + i * 3), accent)
        elif self.p.family == "THERMAL":
            self.rect((x + 3, y + 7, x + 35, y + 27), "#68382C")
            self.rect((x + 6, y + 8, x + 32, y + 25), "#251716")
            self.rect((x + 8, y + 17, x + 30, y + 23), "#D56B2D")
            self.rect((x + 8, y + 17, x + 30, y + 18), "#FFC45B")
            for i in range(6):
                self.rect((x + 8 + i * 4, y + 22 - i % 4, x + 10 + i * 4, y + 27), "#FF7A2E")
        elif self.p.family == "REACTION":
            self.rect((x + 5, y + 5, x + 33, y + 27), "#785B3E")
            self.rect((x + 8, y + 7, x + 30, y + 24), "#251F1B")
            self.rect((x + 9, y + 13, x + 29, y + 23), "#9B682E")
            self.rect((x + 9, y + 13, x + 29, y + 14), "#E0AE5C")
            for cy in range(y + 8, y + 23, 5):
                self.rect((x + 4, cy, x + 8, cy + 2), "#C28A4B")
                self.rect((x + 30, cy, x + 34, cy + 2), "#C28A4B")
        else:
            self.rect((x + 4, y + 4, x + 10, y + 25), "#9D6548")
            self.rect((x + 28, y + 4, x + 34, y + 25), "#9D6548")
            self.rect((x + 10, y + 13, x + 28, y + 25), "#174E68")
            for i in range(6):
                ex, ey = x + 11 + i * 3, y + 8 + (i * 3) % 10
                self.rect((ex, ey, ex + 2, ey + 2), "#A4F3EF" if i % 2 == 0 else "#7D8FE4")


def main() -> None:
    margin, gutter, heading = 16, 16, 26
    cell_w, cell_h = W * SCALE + 20 * SCALE, H * SCALE + heading
    image = Image.new("RGB", (margin * 2 + cell_w * 2 + gutter, margin * 2 + cell_h * 2 + gutter), "#0D1216")
    canvas = ImageDraw.Draw(image)
    title_font = ImageFont.load_default(size=12)
    for i, profile in enumerate(PANELS):
        validate(profile)
        cell_x = margin + (i % 2) * (cell_w + gutter)
        cell_y = margin + (i // 2) * (cell_h + gutter)
        canvas.text((cell_x + 6, cell_y + 5), f"{i + 1}. {profile.name} | {profile.family} | 2x", fill="#E2E8EC", font=title_font)
        panel = Image.new("RGB", (W * SCALE + 20 * SCALE, H * SCALE), "#0D1216")
        Painter(panel, (0, 0), profile).panel()
        panel = panel.resize(panel.size, Image.Resampling.NEAREST)
        image.paste(panel, (cell_x, cell_y + heading))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUT)
    print(f"Collision audit: 0 overlaps ({len(PANELS)} panels)")
    print(f"Wrote {OUT} ({image.width}x{image.height})")


if __name__ == "__main__":
    main()
