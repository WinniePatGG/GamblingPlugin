#!/usr/bin/env python3
"""Generate the small, dependency-free casino resource pack."""

from pathlib import Path
import struct
import zlib
import json

ROOT = Path(__file__).resolve().parents[1] / "resourcepack"
TEXTURES = ROOT / "assets" / "gambling" / "textures" / "item"
MODELS = ROOT / "assets" / "minecraft" / "models" / "item"

PALETTE = {
    "gold": (244, 166, 45, 255),
    "orange": (255, 112, 22, 255),
    "purple": (181, 73, 220, 255),
    "green": (46, 190, 122, 255),
    "red": (224, 61, 71, 255),
    "blue": (57, 145, 221, 255),
    "teal": (38, 196, 183, 255),
    "black": (18, 19, 28, 255),
    "panel": (37, 28, 38, 255),
    "white": (255, 242, 210, 255),
    "card": (250, 240, 218, 255),
}


def png(path, pixels):
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = b"".join(b"\0" + bytes(sum(row, [])) for row in pixels)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def canvas(color):
    return [[list(color) for _ in range(16)] for _ in range(16)]


def rect(image, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(16, y1)):
        for x in range(max(0, x0), min(16, x1)):
            image[y][x] = list(color)


GLYPHS = {
    "A": ["01110", "10001", "10001", "11111", "10001"],
    "1": ["00100", "01100", "00100", "00100", "01110"],
    "2": ["11110", "00001", "01110", "10000", "11111"],
    "3": ["11110", "00001", "01110", "00001", "11110"],
    "4": ["10010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "11110"],
    "6": ["01110", "10000", "11110", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "00100"],
    "8": ["01110", "10001", "01110", "10001", "01110"],
    "9": ["01110", "10001", "01111", "00001", "01110"],
    "0": ["01110", "10001", "10001", "10001", "01110"],
    "J": ["00111", "00010", "00010", "10010", "01100"],
    "Q": ["01110", "10001", "10101", "01110", "00001"],
    "K": ["10001", "10010", "11100", "10010", "10001"],
    "$": ["01110", "10000", "01110", "00001", "11110"],
}


def glyph(image, text, x, y, color, scale=1):
    for char in text:
        pattern = GLYPHS.get(char, [])
        for row, line in enumerate(pattern):
            for col, bit in enumerate(line):
                if bit == "1":
                    rect(image, x + col * scale, y + row * scale,
                         x + (col + 1) * scale, y + (row + 1) * scale, color)
        x += 6 * scale


def icon(name, color, mark):
    image = canvas(PALETTE["black"])
    rect(image, 1, 1, 15, 15, PALETTE["panel"])
    rect(image, 2, 2, 14, 3, color)
    rect(image, 2, 13, 14, 14, color)
    glyph(image, mark, 5 if len(mark) == 1 else 2, 6, color, 1)
    png(TEXTURES / f"{name}.png", image)


def card(name, rank, suit, red):
    image = canvas(PALETTE["black"])
    rect(image, 1, 0, 15, 16, PALETTE["white"])
    rect(image, 2, 1, 14, 15, PALETTE["card"])
    ink = PALETTE["red"] if red else (35, 38, 48, 255)
    glyph(image, rank, 3, 3, ink, 1)
    glyph(image, suit, 6, 9, ink, 1)
    png(TEXTURES / f"card_{name}.png", image)


def model(name):
    (MODELS / f"{name}.json").write_text(json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"gambling:item/{name}"}
    }, indent=2) + "\n")


def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    MODELS.mkdir(parents=True, exist_ok=True)
    (ROOT / "pack.mcmeta").write_text(json.dumps({
        "pack": {
            "pack_format": 15,
            "supported_formats": {"min_inclusive": 15, "max_inclusive": 75},
            "description": "GamblingPlugin casino UI - pixel casino theme"
        }
    }, indent=2) + "\n")

    icons = {
        "slots": ("gold", "S"), "roulette": ("red", "R"),
        "blackjack": ("green", "21"), "mines": ("red", "M"),
        "crash": ("orange", "C"), "wheel": ("purple", "W"),
        "coinflip": ("gold", "C"), "double": ("teal", "2"),
        "case": ("blue", "C"), "bet": ("gold", "$"),
        "hit": ("green", "H"), "double_action": ("gold", "2"),
        "stand": ("red", "S"),
    }
    for name, (color, mark) in icons.items():
        icon(name, PALETTE[color], mark)
        model(name)

    suits = [("spades", "S", False), ("hearts", "H", True),
             ("diamonds", "D", True), ("clubs", "C", False)]
    ranks = ["A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"]
    for suit_index, (suit, symbol, red) in enumerate(suits):
        for rank_index, rank in enumerate(ranks):
            name = f"card_{suit}_{rank.lower()}"
            card(f"{suit}_{rank.lower()}", rank, symbol, red)
            model(name)

    face = canvas(PALETTE["black"])
    rect(face, 1, 0, 15, 16, PALETTE["purple"])
    for i in range(2, 14, 3):
        rect(face, i, 2, i + 1, 14, PALETTE["gold"])
        rect(face, 2, i, 14, i + 1, PALETTE["gold"])
    png(TEXTURES / "card_back.png", face)
    model("card_back")

    overrides = []
    for index, name in enumerate(icons, 1001):
        overrides.append({"predicate": {"custom_model_data": index}, "model": f"gambling:{name}"})
    for suit_index, (suit, _, _) in enumerate(suits):
        for rank_index, rank in enumerate(ranks):
            data = 1200 + suit_index * 13 + rank_index
            overrides.append({"predicate": {"custom_model_data": data},
                              "model": f"gambling:card_{suit}_{rank.lower()}"})
    overrides.append({"predicate": {"custom_model_data": 1253}, "model": "gambling:card_back"})
    (MODELS / "paper.json").write_text(json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "minecraft:item/paper"},
        "overrides": overrides
    }, indent=2) + "\n")


if __name__ == "__main__":
    main()
