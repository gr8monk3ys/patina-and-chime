# Patina & Chime

A vanilla+ Fabric mod that gives copper and amethyst one shared identity: copper ages and each age has its own job, and amethyst hears.

## Language

### Copper

**Copper hopper**:
A hopper made of copper that exists in eight blocks: four weathering stages, each with a waxed version. No stage is ever faster than the vanilla hopper.
_Avoid_: copper funnel, slow hopper

**Stage** (`CopperHopperStage`):
One age of a copper hopper (Fresh, Exposed, Weathered, Oxidized). A stage fixes the ticks per item, whether it pulls from the container above, whether it picks up dropped items, and how redstone affects it.
_Avoid_: tier, level, age (age is the process, stage is the state)

**Weathering**:
Vanilla's random-tick copper oxidation, which moves an unwaxed copper block to its next stage on its own.
_Avoid_: rusting, decay

**Waxed**:
A copper hopper that never ages and behaves like its unwaxed stage. Honeycomb waxes it; an axe removes the wax.
_Avoid_: sealed, locked

**Verdigris**:
The item scraped off old copper. An axe removing a stage of oxidation drops one; right-clicking unwaxed copper with it advances that block one stage.
_Avoid_: patina flakes, rust

### Amethyst

**Tuning fork**:
A copper and amethyst item that, when struck, rings a **Tier** according to the nearest budding amethyst.
_Avoid_: compass, detector

**Tier** (`TuningForkTier`):
The result of a strike: Strong (0 to 8 blocks), Steady (9 to 16), Faint (17 to 32), or None. Each tier has its own sound and action-bar message.
_Avoid_: stage (stage belongs to copper hoppers)

**Shard offers** (`ShardOffers`):
The enchanting table behaviour where amethyst shards replace lapis, cost 2/4/6 instead of 1/2/3, and reveal every enchantment in an offer.
_Avoid_: shard enchanting

**Music tracker** (`MusicTracker`):
The per-level record of where music is playing, as block position to the first tick it no longer counts.
_Avoid_: music manager

**Active music**:
A jukebox while its disc is playing, or a note block for 5 seconds after its last note. Budding amethyst within 8 blocks (**radius**) of active music grows faster.
_Avoid_: nearby sound

**Amethyst note**:
A note block on a Block of Amethyst, which plays a pitched amethyst sound instead of harp.
_Avoid_: amethyst instrument block

**Shatter note** (`ShatterNoteFeature`):
The top note (24) of an amethyst note block, which breaks every full-grown cluster on budding amethyst within the radius, for 2 shards each. Governed by the `patinachime:shatter_note` game rule and a one-second cooldown per note block.
_Avoid_: harvest note

## Relationships

- A **Copper hopper** has exactly one **Stage**; **Waxed** freezes it.
- **Verdigris** advances a **Stage**; scraping with an axe drops **Verdigris**, so the two net to zero.
- **Active music** is recorded in a **Music tracker**; a **Shatter note** also counts as **Active music**.
- A **Tuning fork** strike yields exactly one **Tier**.

## Flagged ambiguities

- "Stage" means a copper hopper's age; the tuning fork's result is a **Tier**.
