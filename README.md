# Patina & Chime

A vanilla+ Fabric mod for Minecraft 26.2 that gives copper and amethyst a shared identity. Copper ages, and each age has its own job. Amethyst hears.

- **Copper hoppers:** cheaper and slower than iron, with a different job at each oxidation stage. Wax locks the one you want.
- **Verdigris:** scrape it off old copper, then use it to age other copper on demand.
- **Tuning fork:** strike it to find budding amethyst nearby.
- **Amethyst at the enchanting table:** shards cost double what lapis does, and show every enchantment in an offer.
- **Music grows crystals:** a jukebox or note block speeds up budding amethyst nearby.
- **Amethyst note block instrument:** put a note block on a Block of Amethyst.
- **Shatter note:** the top note of that instrument harvests crystals nearby, at half yield.

Design: [docs/specs/2026-11-02-first-release-design.md](docs/specs/2026-11-02-first-release-design.md).

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) 0.20.1 or newer for Minecraft 26.2. The game needs Java 25.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2 (0.161.0 or newer) and the [Patina & Chime jar](https://github.com/gr8monk3ys/patina-and-chime/releases/latest) in your `mods` folder.

The mod adds blocks and items, so your client and any server you join both need it. This version only runs on 26.2.

## Copper hoppers

Copper is cheaper than iron, so a copper hopper is slower than a vanilla one at every stage. In exchange, each stage does a different job.

| Stage | Ticks per item | Pulls from the container above | Picks up dropped items | Redstone |
|---|---|---|---|---|
| Copper | 12 | Yes | Yes | Power locks it, like vanilla |
| Exposed | 16 | No | Yes | Power locks it, like vanilla |
| Weathered | 24 | Yes | Yes | Only runs while powered |
| Oxidized | 32 | Yes | No | Power locks it, like vanilla |

A vanilla hopper moves an item every 8 ticks. Copper hoppers age on their own. Wax one with honeycomb to keep its job, and scrape it with an axe to step it back. Its items stay put through every change.

## Shatter note

Put a note block on a Block of Amethyst and set it to its top note, 24 right-clicks from the lowest. Playing that note breaks every full-grown cluster on budding amethyst within 8 blocks. Each cluster drops 2 shards, where a pickaxe gets 4, and a copper hopper underneath can collect them. An oxidized one won't, because it doesn't pick up dropped items. Buds, budding amethyst and clusters on any other block are left alone.

- Redstone or a left-click at the top note fires it.
- A right-click fires it when it steps the note from 23 up to the top. A right-click at the top wraps round to the lowest note and breaks nothing.
- Each note block can do this once per second, and the note still counts as music in between.

There's no option to turn this off, and land-claim mods may not stop it, so a player outside a claim could harvest a farm inside one. They'd get half the yield and the crystals grow back.

## Recipes

| Makes | From |
|---|---|
| Copper hopper | 5 copper ingots and a chest, in the shape of a vanilla hopper |
| Tuning fork | 3 copper ingots and an amethyst shard: copper in both top corners and bottom middle, the shard in the centre |
| 2 verdigris | A copper ingot and a water bucket, which gives the empty bucket back |
| Cyan dye | A verdigris |
| 4 amethyst shards | A Block of Amethyst. Four shards still make a block, as in vanilla |
| Waxed copper hopper | A copper hopper of any stage and a honeycomb |

Scraping an aged copper block or hopper with an axe also drops a verdigris.

## Building

Needs JDK 25.

```bash
./gradlew build
```

```bash
./gradlew runGameTest
```

```bash
./gradlew runClientGameTest
```

Changes between versions are in [CHANGELOG.md](CHANGELOG.md).

## Licence

MIT. See [LICENSE](LICENSE).
