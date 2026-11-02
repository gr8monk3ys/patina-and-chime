"""Copper hopper textures: riveted rim, ribbed funnel, patina creeping out from the rivets and seams as the stage ages.

Writes three block textures and one item texture per stage. Waxed blocks reuse the textures of their stage.
"""
import pathlib

from palette import COPPER, TRANSPARENT
from png import write_png

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/patinachime/textures"
SIZE = 16
RIVETS = ((2, 2), (13, 2))

# (name, chance a pixel takes a fleck, fleck tones, chance of a drip below each rivet, drip length)
STAGES = {
    "": ("fresh", 0.10, [("fresh", 0), ("fresh", 3)], 0),
    "exposed_": ("exposed", 0.14, [("exposed", 0), ("weathered", 3), ("weathered", 2)], 1),
    "weathered_": ("weathered", 0.16, [("exposed", 2), ("oxidized", 1), ("weathered", 0)], 2),
    "oxidized_": ("oxidized", 0.12, [("oxidized", 0), ("weathered", 3)], 3),
}


def noise(x, y, seed):
    value = (x * 73856093) ^ (y * 19349663) ^ ((seed + 1) * 83492791)
    value = (value ^ (value >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((value ^ (value >> 16)) & 0xFFFF) / 0xFFFF


def flecked(rows, stage, seed, mask):
    _, chance, tones, _ = stage
    for y in range(SIZE):
        for x in range(SIZE):
            if rows[y][x][3] and mask(x, y) and noise(x, y, seed) < chance:
                name, tone = tones[int(noise(y, x, seed + 7) * len(tones)) % len(tones)]
                rows[y][x] = COPPER[name][tone]
    return rows


def outside(stage):
    name, _, tones, drip = stage
    ramp = COPPER[name]
    rows = [[ramp[4] for _ in range(SIZE)] for _ in range(SIZE)]

    for x in range(SIZE):
        rows[0][x] = ramp[0] if x < 15 else ramp[1]
        for y in (1, 2):
            rows[y][x] = ramp[1] if x < 10 else ramp[2]
        rows[3][x] = ramp[2]
        rows[4][x] = ramp[3]
        rows[5][x] = ramp[4]
    for y in (1, 2, 3):
        rows[y][7] = ramp[3]

    for x in range(4, 12):
        rows[6][x] = ramp[4]
        rows[11][x] = ramp[3]
        for y in (7, 8, 10):
            rows[y][x] = ramp[1] if x < 6 else ramp[2] if x < 10 else ramp[3]
        rows[9][x] = ramp[0] if x < 6 else ramp[1] if x < 10 else ramp[2]

    for x in range(6, 10):
        rows[12][x] = ramp[4]
        rows[15][x] = ramp[3]
        for y in (13, 14):
            rows[y][x] = ramp[1] if x == 6 else ramp[2] if x < 9 else ramp[3]

    flecked(rows, stage, 11, lambda x, y: y <= 3 or 7 <= y <= 10 and 4 <= x <= 11 or 13 <= y <= 14 and 6 <= x <= 9)

    for x, y in RIVETS:
        rows[y][x] = ramp[0]
        rows[y + 1][x] = ramp[4]
        if drip:
            for step in range(drip):
                if y + 2 + step <= 4:
                    rows[y + 2 + step][x] = COPPER[tones[1][0]][tones[1][1]]
    return rows


def top(stage):
    ramp = COPPER[stage[0]]
    rows = [[ramp[2] for _ in range(SIZE)] for _ in range(SIZE)]
    for i in range(SIZE):
        rows[0][i] = ramp[0]
        rows[i][0] = ramp[0]
        rows[1][i] = ramp[1]
        rows[i][1] = ramp[1]
        rows[15][i] = ramp[2]
        rows[i][15] = ramp[2]
        rows[14][i] = ramp[1]
        rows[i][14] = ramp[1]
    flecked(rows, stage, 23, lambda x, y: True)
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        rows[y][x] = ramp[3]
    return rows


def inside(stage):
    ramp = COPPER[stage[0]]
    rows = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            depth = max(abs(x - 7.5), abs(y - 7.5))
            row.append(ramp[2] if depth > 5.5 else ramp[3] if depth > 3.5 else ramp[4])
        rows.append(row)
    return flecked(rows, stage, 37, lambda x, y: max(abs(x - 7.5), abs(y - 7.5)) > 3.5)


def item(stage):
    ramp = COPPER[stage[0]]
    rows = [[TRANSPARENT for _ in range(SIZE)] for _ in range(SIZE)]

    def paint(y, left, right, tones):
        width = right - left
        for x in range(left, right + 1):
            position = (x - left) / width
            rows[y][x] = tones[0] if position < 0.3 else tones[1] if position < 0.75 else tones[2]
        rows[y][left] = ramp[4] if y > 2 else tones[0]
        rows[y][right] = ramp[4]

    paint(2, 1, 14, (ramp[0], ramp[0], ramp[1]))
    paint(3, 1, 14, (ramp[1], ramp[1], ramp[2]))
    paint(4, 1, 14, (ramp[1], ramp[2], ramp[2]))
    paint(5, 1, 14, (ramp[3], ramp[3], ramp[3]))
    paint(6, 1, 14, (ramp[4], ramp[4], ramp[4]))
    paint(7, 3, 12, (ramp[1], ramp[2], ramp[3]))
    paint(8, 3, 12, (ramp[0], ramp[1], ramp[2]))
    paint(9, 4, 11, (ramp[1], ramp[2], ramp[3]))
    paint(10, 5, 10, (ramp[2], ramp[3], ramp[3]))
    paint(11, 6, 9, (ramp[4], ramp[4], ramp[4]))
    paint(12, 6, 9, (ramp[1], ramp[2], ramp[3]))
    paint(13, 6, 9, (ramp[1], ramp[2], ramp[3]))
    paint(14, 6, 9, (ramp[3], ramp[3], ramp[4]))

    flecked(rows, stage, 41, lambda x, y: 2 <= y <= 4 and 2 <= x <= 13 or 7 <= y <= 9 and 4 <= x <= 11)
    rows[3][3] = ramp[0]
    rows[4][3] = ramp[4]
    rows[3][12] = ramp[0]
    rows[4][12] = ramp[4]
    return rows


def main():
    for prefix, stage in STAGES.items():
        name = f"{prefix}copper_hopper"
        write_png(ROOT / "block" / f"{name}_outside.png", outside(stage))
        write_png(ROOT / "block" / f"{name}_top.png", top(stage))
        write_png(ROOT / "block" / f"{name}_inside.png", inside(stage))
        write_png(ROOT / "item" / f"{name}.png", item(stage))


if __name__ == "__main__":
    main()
