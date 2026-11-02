# Patina & Chime

A vanilla+ Fabric mod for Minecraft 26.2 that gives copper and amethyst a shared identity. Copper ages, and each age has its own job. Amethyst hears.

- **Copper hoppers:** cheaper and slower than iron, with a different job at each oxidation stage. Wax locks the one you want.
- **Verdigris:** scrape it off old copper, then use it to age other copper on demand.
- **Tuning fork:** strike it to find budding amethyst nearby.
- **Amethyst at the enchanting table:** shards cost double what lapis does, and show every enchantment in an offer.
- **Music grows crystals:** a jukebox or note block speeds up budding amethyst nearby.
- **Amethyst note block instrument:** put a note block on a Block of Amethyst.

Design: [docs/specs/2026-11-02-first-release-design.md](docs/specs/2026-11-02-first-release-design.md).

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) 0.20.1 or newer for Minecraft 26.2. The game needs Java 25.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2 and the [Patina & Chime jar](https://github.com/gr8monk3ys/patina-and-chime/releases/latest) in your `mods` folder.

The mod adds blocks and items, so your client and any server you join both need it. This version only runs on 26.2.

## Building

Needs JDK 25.

```bash
./gradlew build
```

```bash
./gradlew runGameTest
```

## Licence

MIT. See [LICENSE](LICENSE).
