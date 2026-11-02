"""Writes the verdigris and tuning fork item textures and the mod icon.

Run from anywhere: python tools/gen_item_textures.py
"""
from pathlib import Path

from palette import AMETHYST, COPPER, TRANSPARENT
from png import write_png

ASSETS = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "assets" / "patinachime"

OXIDIZED = COPPER["oxidized"]
FRESH = COPPER["fresh"]

VERDIGRIS_KEY = {str(index): color for index, color in enumerate(OXIDIZED)}
FORK_KEY = {str(index): color for index, color in enumerate(FRESH)}
FORK_KEY.update({"abcde"[index]: color for index, color in enumerate(AMETHYST)})

VERDIGRIS = [
    "................",
    "................",
    ".........01.....",
    "........0123....",
    "...01...1233....",
    "..0123...344....",
    "..1234.......0..",
    "...344...00.012.",
    ".......0122.1234",
    "...01..12334.34.",
    "..0123..2344....",
    ".012234..44.....",
    ".12334......012.",
    "..344......1234.",
    "............34..",
    "................",
]

TUNING_FORK = [
    "................",
    "...........01...",
    "..........013...",
    ".........013..0.",
    "........013..013",
    ".......013..013.",
    "......013..013..",
    "......13..013...",
    "......4.0123....",
    ".....0ab1234....",
    "....01bcd34.....",
    "...013.d44......",
    "..013...........",
    ".013............",
    ".34.............",
    "................",
]

ICON_SIZE = 128
ICON_SCALE = 7
ICON_MARGIN = (ICON_SIZE - 16 * ICON_SCALE) // 2
ICON_BACKGROUND = OXIDIZED[2]
ICON_EDGE = OXIDIZED[4]
ICON_SHADOW = OXIDIZED[3]
ICON_EDGE_WIDTH = 4


def render(art, key):
    return [[key[pixel] if pixel != "." else TRANSPARENT for pixel in row] for row in art]


def icon(art, key):
    rows = []
    for y in range(ICON_SIZE):
        row = []
        for x in range(ICON_SIZE):
            row.append(icon_pixel(art, key, x, y))
        rows.append(row)
    return rows


def icon_pixel(art, key, x, y):
    if min(x, y, ICON_SIZE - 1 - x, ICON_SIZE - 1 - y) < ICON_EDGE_WIDTH:
        return ICON_EDGE
    art_x = (x - ICON_MARGIN) // ICON_SCALE
    art_y = (y - ICON_MARGIN) // ICON_SCALE
    if opaque(art, art_x, art_y):
        return key[art[art_y][art_x]]
    if opaque(art, art_x - 1, art_y - 1):
        return ICON_SHADOW
    return ICON_BACKGROUND


def opaque(art, x, y):
    return 0 <= y < len(art) and 0 <= x < len(art[y]) and art[y][x] != "."


def main():
    write_png(ASSETS / "textures" / "item" / "verdigris.png", render(VERDIGRIS, VERDIGRIS_KEY))
    write_png(ASSETS / "textures" / "item" / "tuning_fork.png", render(TUNING_FORK, FORK_KEY))
    write_png(ASSETS / "icon.png", icon(TUNING_FORK, FORK_KEY))


if __name__ == "__main__":
    main()
