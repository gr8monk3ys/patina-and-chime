# Patina & Chime

Fabric mod for Minecraft 26.2 (Java 25). The design is in `docs/specs/`; read it before changing behaviour.

## Layout

- Each feature has its own package under `src/main/java/io/github/gr8monk3ys/patinachime/` and a static `init()` called from `PatinaChime`.
- Each feature group has its own mixin config, so features never share one.
- Client-only code goes in `src/client/`. GameTests go in `src/gametest/`.
- Texture scripts live in `tools/`, use only the Python standard library, and are deterministic. Commit both the script and the PNGs it writes.

## Commands

```bash
./gradlew build
```

```bash
./gradlew runGameTest
```

```bash
./gradlew runClientGameTest
```

To run alongside a popular mod that touches the same code, add `-Pcompat=lithium` or `-Pcompat=easymagic` to a run.

Before a release, run the same suites against the jar a player installs, on a real dedicated server and client:

```bash
./gradlew productionGameTest
```

```bash
./gradlew productionClientGameTest
```

## Rules

- No copper hopper stage may be faster than the vanilla hopper (8 ticks per item).
- All art is original: never copy or recolour Mojang's pixels.
- Audio reuses vanilla sounds by id, so no audio files ship with the mod.
- Mojang's mappings are used directly. Minecraft 26.x is not obfuscated, so there is no remapping.
- Never run `./gradlew --stop`: it stops every daemon on the machine, including other sessions' builds.
