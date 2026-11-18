import json
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/earth_on_minecraft"
MODELS = ROOT / "models/block"
ITEMS = ROOT / "items"

machines = {
    "hand_crank": ("wood", "metal", [([1, 0, 1], [15, 3, 15], "body"), ([6, 3, 6], [10, 12, 10], "metal"), ([3, 10, 6], [13, 12, 10], "metal"), ([11, 7, 6], [13, 10, 10], "wood"), ([11, 7, 7], [13, 10, 9], "wood")]),
    "forge": ("hot", "brick", [([1, 0, 1], [15, 12, 15], "body"), ([3, 2, 0], [13, 10, 3], "hot"), ([4, 3, 3], [12, 8, 5], "fire"), ([0, 12, 0], [16, 16, 16], "trim")]),
    "grindstone": ("stone", "wood", [([2, 0, 2], [5, 12, 5], "frame"), ([11, 0, 2], [14, 12, 5], "frame"), ([1, 12, 1], [15, 14, 6], "frame"), ([6, 4, 5], [10, 12, 13], "stone"), ([4, 3, 4], [12, 5, 12], "metal")]),
    "hammer": ("metal", "hot", [([2, 0, 3], [14, 2, 13], "base"), ([4, 2, 5], [6, 12, 11], "metal"), ([10, 2, 5], [12, 12, 11], "metal"), ([3, 12, 4], [13, 15, 12], "metal"), ([5, 14, 6], [11, 16, 10], "hot")]),
    "tongs": ("metal", "stone", [([1, 0, 2], [15, 3, 14], "base"), ([3, 3, 4], [5, 12, 12], "metal"), ([11, 3, 4], [13, 12, 12], "metal"), ([4, 10, 6], [12, 12, 10], "metal")]),
    "mold": ("stone", "metal", [([0, 0, 0], [16, 3, 16], "frame"), ([0, 13, 0], [16, 16, 16], "metal"), ([0, 3, 0], [3, 13, 16], "metal"), ([13, 3, 0], [16, 13, 16], "metal"), ([3, 3, 3], [13, 13, 13], "cavity")]),
    "wire_die": ("metal", "hot", [([2, 0, 2], [14, 3, 14], "base"), ([3, 3, 3], [13, 12, 13], "metal"), ([5, 12, 5], [11, 15, 11], "hot"), ([6, 7, 0], [10, 11, 3], "metal")]),
    "quench_tank": ("wet", "metal", [([0, 0, 0], [16, 3, 16], "body"), ([0, 3, 0], [3, 14, 16], "metal"), ([13, 3, 0], [16, 14, 16], "metal"), ([3, 3, 0], [13, 14, 3], "metal"), ([3, 3, 13], [13, 14, 16], "metal"), ([3, 4, 3], [13, 5, 13], "liquid")]),
    "survey_stake": ("wood", "metal", [([6, 0, 6], [10, 13, 10], "body"), ([4, 12, 4], [12, 16, 12], "metal"), ([5, 5, 5], [11, 6, 11], "metal")]),
    "oven": ("hot", "brick", [([1, 0, 1], [15, 12, 15], "body"), ([3, 12, 2], [13, 16, 14], "brick"), ([3, 2, 0], [13, 10, 3], "hot"), ([5, 3, 3], [11, 8, 5], "fire"), ([0, 0, 0], [16, 2, 16], "trim")]),
    "oven_mouth": ("hot", "brick", [([0, 0, 0], [3, 16, 16], "brick"), ([13, 0, 0], [16, 16, 16], "brick"), ([3, 13, 0], [13, 16, 16], "brick"), ([3, 0, 0], [5, 13, 16], "trim"), ([11, 0, 0], [13, 13, 16], "trim"), ([5, 0, 1], [11, 2, 15], "hot")]),
    "chimney": ("hot", "metal", [([4, 0, 4], [12, 14, 12], "body"), ([2, 13, 2], [14, 15, 14], "metal"), ([1, 15, 1], [15, 16, 15], "trim")]),
    "tuyere": ("hot", "metal", [([2, 4, 2], [14, 12, 14], "body"), ([0, 3, 3], [3, 13, 13], "metal"), ([13, 3, 3], [16, 13, 13], "trim"), ([3, 6, 5], [13, 10, 11], "hot")]),
    "air_inlet": ("modern", "metal", [([1, 1, 1], [15, 15, 15], "body"), ([3, 4, 0], [13, 5, 2], "metal"), ([3, 7, 0], [13, 8, 2], "metal"), ([3, 10, 0], [13, 11, 2], "metal"), ([0, 0, 0], [16, 2, 16], "trim")]),
    "bore_head": ("metal", "hot", [([4, 0, 4], [12, 5, 12], "body"), ([5, 5, 5], [11, 11, 11], "metal"), ([6, 11, 6], [10, 15, 10], "hot"), ([5, 14, 5], [11, 16, 11], "metal")]),
    "quarry": ("modern", "metal", [([0, 0, 1], [3, 16, 4], "frame"), ([13, 0, 1], [16, 16, 4], "frame"), ([0, 13, 1], [16, 16, 4], "metal"), ([1, 0, 12], [4, 16, 15], "frame"), ([12, 0, 12], [15, 16, 15], "frame"), ([3, 5, 6], [13, 8, 10], "body")]),
    "autoclave": ("hot", "metal", [([1, 3, 2], [15, 13, 14], "body"), ([0, 5, 4], [2, 11, 12], "trim"), ([14, 5, 4], [16, 11, 12], "metal"), ([11, 10, 1], [15, 15, 5], "detail"), ([4, 0, 4], [12, 3, 12], "metal")]),
    "carbonator": ("modern", "metal", [([3, 1, 3], [13, 14, 13], "body"), ([2, 0, 2], [14, 3, 14], "metal"), ([2, 13, 2], [14, 16, 14], "trim"), ([6, 14, 6], [10, 16, 10], "detail")]),
    "coffee_grinder": ("modern", "wood", [([2, 0, 2], [14, 4, 14], "base"), ([4, 4, 4], [12, 9, 12], "body"), ([3, 9, 3], [13, 13, 13], "metal"), ([7, 13, 7], [9, 15, 9], "metal"), ([8, 14, 8], [15, 15, 9], "wood")]),
    "juice_press": ("wet", "metal", [([1, 0, 1], [15, 3, 15], "base"), ([2, 3, 2], [4, 14, 4], "frame"), ([12, 3, 2], [14, 14, 4], "frame"), ([3, 11, 3], [13, 13, 13], "metal"), ([6, 5, 6], [10, 11, 10], "detail")]),
    "pasteurizer": ("wet", "metal", [([1, 1, 1], [15, 10, 15], "body"), ([0, 10, 0], [16, 13, 16], "metal"), ([3, 13, 3], [13, 15, 13], "trim"), ([12, 11, 2], [15, 15, 5], "detail")]),
    "rolling_mill": ("modern", "metal", [([1, 0, 2], [4, 16, 14], "frame"), ([12, 0, 2], [15, 16, 14], "frame"), ([4, 6, 3], [12, 10, 13], "metal"), ([4, 4, 4], [12, 7, 12], "roller"), ([4, 10, 4], [12, 13, 12], "roller"), ([0, 0, 1], [16, 2, 15], "trim")]),
    "battery_box": ("power", "metal", [([1, 0, 1], [15, 15, 15], "body"), ([3, 15, 3], [6, 16, 6], "pole"), ([10, 15, 3], [13, 16, 6], "pole"), ([10, 7, 0], [14, 12, 2], "detail"), ([0, 0, 0], [16, 2, 16], "trim")]),
    "control_panel": ("modern", "metal", [([1, 0, 1], [15, 7, 15], "body"), ([2, 7, 2], [14, 10, 14], "panel"), ([3, 10, 3], [13, 15, 13], "panel"), ([4, 11, 1], [7, 14, 3], "detail"), ([9, 11, 1], [12, 14, 3], "detail")]),
    "industrial_machine_casing": ("modern", "metal", [([0, 0, 0], [16, 16, 16], "body"), ([0, 0, 0], [2, 16, 16], "trim"), ([14, 0, 0], [16, 16, 16], "metal"), ([2, 0, 0], [14, 2, 16], "metal"), ([2, 14, 0], [14, 16, 16], "trim")]),
    "machine_input_interface": ("modern", "metal", [([0, 0, 0], [16, 16, 16], "body"), ([3, 3, 0], [13, 13, 2], "metal"), ([4, 4, 2], [12, 12, 4], "detail"), ([5, 5, 4], [11, 11, 6], "trim")]),
    "machine_output_interface": ("power", "metal", [([0, 0, 0], [16, 16, 16], "body"), ([3, 3, 14], [13, 13, 16], "metal"), ([4, 4, 12], [12, 12, 14], "detail"), ([5, 5, 10], [11, 11, 12], "trim")]),
    "steel_process_pipe": ("modern", "metal", [([5, 5, 0], [11, 11, 16], "body"), ([3, 3, 0], [13, 5, 2], "metal"), ([3, 11, 0], [13, 13, 2], "metal"), ([3, 5, 0], [5, 11, 2], "trim"), ([11, 5, 0], [13, 11, 2], "metal"), ([5, 5, 0], [11, 11, 2], "detail")]),
}

material_textures = {
    "modern": ("craft_steel_brushed", "craft_steel_top"),
    "hot": ("craft_firebrick", "craft_firebrick_top"),
    "wet": ("craft_water_quench", "craft_water_top"),
    "power": ("craft_copper_wiring", "craft_copper_top"),
    "wood": ("craft_wood_oak", "craft_wood_oak_top"),
    "stone": ("craft_stone_mill", "craft_stone_mill_top"),
    "metal": ("craft_iron_cast", "craft_iron_cast_top"),
    "brick": ("craft_firebrick", "craft_firebrick_top"),
}

MATERIAL_COLORS = {
    "craft_steel_brushed": (118, 132, 139), "craft_steel_top": (145, 157, 161),
    "craft_firebrick": (133, 57, 39), "craft_firebrick_top": (165, 78, 50),
    "craft_water_quench": (35, 115, 145), "craft_water_top": (57, 151, 172),
    "craft_copper_wiring": (166, 84, 42), "craft_copper_top": (197, 113, 55),
    "craft_wood_oak": (126, 79, 38), "craft_wood_oak_top": (156, 103, 55),
    "craft_stone_mill": (108, 105, 98), "craft_stone_mill_top": (137, 134, 124),
    "craft_iron_cast": (74, 80, 83), "craft_iron_cast_top": (101, 108, 110),
}

def write_texture(path: Path, base: tuple[int, int, int]) -> None:
    rows = []
    for y in range(16):
        row = bytearray([0])
        for x in range(16):
            step = ((x * 7 + y * 11 + (x * y) % 5) % 20) - 9
            grain = 5 if (x + y) % 7 == 0 else 0
            rgb = tuple(max(0, min(255, c + step * 3 + grain)) for c in base)
            row.extend((*rgb, 255))
        rows.append(bytes(row))
    def chunk(kind: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
    signature = bytes([137, 80, 78, 71, 13, 10, 26, 10])
    png = signature + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + chunk(b"IEND", b"")
    path.write_bytes(png)

for texture_name, color in MATERIAL_COLORS.items():
    write_texture(ROOT / "textures/block" / f"{texture_name}.png", color)

for name, (base_material, accent_material, boxes) in machines.items():
    channel_materials = {
        "body": base_material, "base": accent_material, "frame": accent_material,
        "metal": "metal", "trim": accent_material, "detail": "power",
        "panel": "wet", "roller": "metal", "pole": "metal", "fire": "hot",
        "cavity": "stone", "liquid": "wet", "wood": "wood", "hot": "hot",
        "brick": "brick", "stone": "stone",
    }
    textures = {"particle": f"earth_on_minecraft:block/{material_textures[base_material][0]}"}
    for _, _, channel in boxes:
        material = channel_materials[channel]
        tex = material_textures[material][1 if channel in ("trim", "detail", "panel", "fire", "liquid", "cavity", "hot", "brick") else 0]
        textures[channel] = f"earth_on_minecraft:block/{tex}"
    elements = []
    for frm, to, channel in boxes:
        elements.append({"from": frm, "to": to, "faces": {face: {"texture": f"#{channel}"} for face in ("down", "up", "north", "south", "west", "east")}})
    (MODELS / f"{name}.json").write_text(json.dumps({"parent": "minecraft:block/block", "textures": textures, "elements": elements}, indent=2) + "\n", encoding="utf-8")
    item = ITEMS / f"{name}.json"
    item.write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"earth_on_minecraft:block/{name}"}}, indent=2) + "\n", encoding="utf-8")

for name in ("copper_power_cable", "heavy_copper_power_cable", "thin_copper_power_cable", "control_panel_active", "industrial_machine_casing_assembled", "steel_process_pipe_assembled"):
    path = MODELS / f"{name}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["parent"] = "minecraft:block/block"
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
