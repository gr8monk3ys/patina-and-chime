# Shatter note design

Date: 2026-11-02. Planned for 0.2.0, after 0.1.3.

## Goal

Give amethyst farms a hands-free harvest that fits "amethyst hears": one high note breaks every full-grown crystal around it, at half the yield of a pickaxe.

Music growth already makes crystals grow faster. With this, one note block can grow a farm and harvest it, so the two features form a loop.

No option should be simply best. A pickaxe gives 4 or more shards per cluster and takes effort. The note gives 2 and takes none.

## The rule

A shatter note fires when all of these are true:

- The block is a note block with the harp instrument on a Block of Amethyst. This is the same check the amethyst note instrument uses, so a mob head on top or a real instrument below rules it out.
- Its note is 24, the top pitch.
- The note actually sounds. The hook is `NoteBlock.triggerEvent` returning true, the same place that records music, so a block swapped out before the queued event runs doesn't fire. A note block with something above it never gets that far, as in vanilla.
- It's running on the server.

Redstone and clicks both work, since both reach `triggerEvent`. A left-click plays the current note, so it fires at 24. A right-click changes the note first, so it fires from 23 and not from 24, which wraps to 0.

## What it breaks

A cluster breaks when all of these are true:

- It is `amethyst_cluster`. Small, medium and large buds stay.
- It is attached to `budding_amethyst`, meaning the block behind its `FACING` is budding amethyst. Clusters placed as decoration on anything else are never touched.
- It is within 8 blocks of the note block, using the same sphere and `RADIUS` as music growth. Exactly 8 blocks counts.

Budding amethyst never breaks.

## What it drops

Each cluster breaks through `destroyBlock(pos, true)` with no tool, so vanilla's loot table gives 2 shards. That table gives 4 with a pickaxe, plus Fortune, and 2 otherwise, which is also what a piston break gives. Breaking uses the normal sound and particles, and the `block_drops` game rule still applies.

Shards drop where the cluster was, so a copper hopper can collect them. An oxidized hopper won't, because it doesn't pick up dropped items.

The top note still counts as music for growth.

## Implementation

Break clusters as soon as the note plays. Add a `ShatterNoteFeature` to the `music` package, called from the existing `triggerEvent` hook in `NoteBlockMixin`, so no new mixin config is needed.

Alternatives rejected:

- **Breaking on later random ticks:** slightly cheaper, but crystals would break at random times after the note, so the cause would be hard to see.
- **Vanilla's vibration system:** clusters have no block entity to listen with, so this would add a lot of machinery for no gain.

Search order:

1. Stop unless the note qualifies, so ordinary notes cost one state check.
2. Walk the chunk sections the sphere touches, skipping any chunk that isn't loaded and any section where `LevelChunkSection.maybeHas` finds no cluster. The search never loads a chunk.
3. Collect the matching positions first, then break them, checking each is still a cluster, so block updates during breaking can't disturb the search.

## Performance

The plan was no cooldown unless 16 qualifying note blocks firing as fast as redstone allows cost more than 1 ms per game tick. They measured 0.8 to 1.3 ms across runs, so each note block now has a one-second cooldown, stored in a `MusicTracker` and not saved. With it, the worst search costs about 0.1 to 0.2 ms per game tick for 16 note blocks.

Those figures time the search only, not the block breaking that follows it. A repeat note within the second doesn't search, and still counts as music.

## Servers

There's no off switch, keeping to the first release's no-config decision. The worst a griefer can do is harvest a farm at half yield, which a piston can already do, and the crystals grow back. Land-claim mods may not stop it, so the listing must say so.

## Testing

Fabric GameTests, written before the code where practical:

| Case | Expected |
|---|---|
| Top note, by left-click and by redstone, cluster on budding amethyst | Cluster breaks and drops exactly 2 shards |
| Right-click at note 23 | Cluster breaks |
| Right-click at note 24 | Nothing breaks |
| Cluster whose budding amethyst is in an unloaded chunk | Stays, and that chunk stays unloaded |
| Cluster exactly 8 blocks away | Breaks |
| Cluster just outside 8 blocks | Stays |
| Buds of every size | Stay |
| Budding amethyst | Stays |
| Cluster on any other block | Stays |
| Note 23 | Nothing breaks |
| Note block on another block, at note 24 | Nothing breaks |
| Mob head on top | Nothing breaks |
| Block above the note block | Nothing breaks |
| Sphere reaching an unloaded chunk | The chunk stays unloaded |
| Fresh copper hopper under a cluster | Collects the shards |
| Oxidized copper hopper under a cluster | Leaves them |
| Second top note within a second | Nothing breaks, until the second has passed |

Run the game and client suites plain and on the real jar before release, and the compatibility mods as for 0.1.2.

Automated tests can't judge these, so they need a hand test:

- Whether breaking a whole geode at once sounds good or just loud.
- Whether 8 blocks feels right or too strong.
- Whether the note, the recipe and music growth together make shards too easy. The block recipe already raised the supply.

## Docs

Update the README, both listings, the changelog and the testing guide. Add the land-claim note to both listings.

## Left out on purpose

- A gamerule or config file.
- Breaking buds, or any note sequence. Sequences belong to the melody lock idea.
- New blocks, items, art or audio files.
