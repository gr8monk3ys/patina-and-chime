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

To run alongside popular mods, add `-Pcompat=lithium` or `-Pcompat=sodium,jade` to a run. The names are at the top of `build.gradle`.

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

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, through the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

The five default labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root, created when first needed. See `docs/agents/domain.md`.
