# Shatter note implementation plan

Builds [the design](2026-11-02-shatter-note-design.md) for 0.2.0. Work on a new branch `feat/shatter-note` cut from `design/shatter-note`, and open its PR against `feat/amethyst-block-to-shards`, or against `main` if 0.1.3 has merged by then. Don't merge, tag or publish without the owner's say.

Gradle needs `JAVA_HOME=C:/Users/loren/.jdks/temurin-25`. Never run `./gradlew --stop`. New Java files use tabs, and existing files keep their line endings.

## What already exists

- `BuddingAmethystScan` walks chunk sections with `getChunkNow` and `maybeHas`, and never loads a chunk. Copy its shape.
- `NoteBlockMixin.patinachime$recordNote` runs at the `RETURN` of `triggerEvent`. Notes reach it on the client too, so keep the `ServerLevel` check.
- `AmethystNoteFeature.noteSound` holds the amethyst note check, and `MusicTracker` holds the sphere check.
- `TuningForkGameTests` shows both test patterns needed here: one-tick tests in chunks far from every test structure, and a timing test.

## Traps to design around

1. **Right-clicking changes the note before playing it.** A right-click at 23 moves to 24 and fires. A right-click at 24 wraps to 0 and doesn't. A left-click plays the current note, so it fires at 24. Test all three, and say so in the player docs.
2. **`level.getBlockState` loads chunks.** A cluster at a chunk edge can have its budding amethyst in an unloaded chunk. Check `level.isLoaded(backing)` first, and treat an unloaded backing block as not budding.
3. **Other tests' crystals can be in range.** `MusicGrowthGameTests` grows clusters on budding amethyst, and test areas can be only 13 blocks apart. Shatter tests that take several ticks go in their own environment, so they run in a separate batch. Tests that fit in one tick stay far away instead. Check the log once to confirm the batches really are separate.
4. **Broken clusters leave item entities.** Far-away tests must remove them, or a later test's count is wrong.

## Task 1: Test harness and the first failing test

Add `ShatterNoteGameTests` to the `fabric-gametest` entrypoints in `src/gametest/resources/fabric.mod.json`. Add `test_environment/shatter_note.json` with the same content as `hopper_no_random_ticks.json`.

Helpers, using a far center like `TuningForkGameTests`, with chunks loaded by `loadAround`:

```java
private static void placeNote(ServerLevel level, BlockPos note, Block below, int pitch)
private static void placeCluster(ServerLevel level, BlockPos cluster, Direction facing, Block backing)
private static boolean fire(ServerLevel level, BlockPos note) // state.triggerEvent(level, note, 0, 0)
private static int shardsAround(ServerLevel level, BlockPos center) // counts stack sizes, then discards the entities
```

First test: `topNoteBreaksAClusterOnBuddingAmethyst` asserts the cluster is gone, the budding amethyst stays and exactly 2 shards dropped. Run `./gradlew runGameTest` and confirm it fails because the cluster is still there.

## Task 2: The feature

Make the smallest changes that pass, then keep going:

- `AmethystNoteFeature`: extract `isAmethystNote(BlockGetter, BlockPos)` from `noteSound`, with no change in behaviour.
- `MusicTracker`: add a public static `isWithinRadius(BlockPos, BlockPos)` beside the existing check, sharing its arithmetic.
- `ShatterNoteFeature` in the `music` package, with an empty `init()` called from `PatinaChime`:

```java
public static final int TOP_NOTE = 24;
private static final int RANGE = MusicTracker.RADIUS;
private static final Predicate<BlockState> IS_CLUSTER = state -> state.is(Blocks.AMETHYST_CLUSTER);

public static void onNotePlayed(ServerLevel level, BlockPos note) {
	if (AmethystNoteFeature.isAmethystNote(level, note) && level.getBlockState(note).getValue(NoteBlock.NOTE) == TOP_NOTE) {
		for (BlockPos cluster : grownClustersAround(level, note)) {
			if (level.getBlockState(cluster).is(Blocks.AMETHYST_CLUSTER)) {
				level.destroyBlock(cluster, true);
			}
		}
	}
}

private static boolean isOnBudding(ServerLevel level, BlockPos cluster, BlockState state) {
	BlockPos backing = cluster.relative(state.getValue(AmethystClusterBlock.FACING).getOpposite());
	return level.isLoaded(backing) && level.getBlockState(backing).is(Blocks.BUDDING_AMETHYST);
}
```

`grownClustersAround` has the same section walk as `BuddingAmethystScan`, returns a new list and uses `MusicTracker`'s sphere check. Don't share the walk with the scan yet: the two differ in their predicate and result, and a third user would be the time to extract it.

- `NoteBlockMixin.patinachime$recordNote`: after `onNotePlayed`, call the new feature when `level` is a `ServerLevel`.

## Task 3: Finish the rule's tests

One-tick tests, each with its own failure message:

| Test | Expected |
|---|---|
| Cluster at (8, 0, 0) | Breaks |
| Cluster at (8, 1, 0) and at (6, 6, 0) | Both stay, as they're outside the sphere |
| Small, medium and large buds | Stay |
| Budding amethyst | Stays |
| Cluster on stone, and on a Block of Amethyst | Stay |
| Note 23 | Nothing breaks |
| Note 24 on dirt | Nothing breaks |
| Zombie head on top | Nothing breaks |
| Stone above | `playNote` never queues the event, so test it through a real play, not `fire` |
| `block_drops` off | Cluster breaks and drops nothing. Restore the rule in the same tick |
| Several clusters | All break and shards total 2 each |
| Cluster's budding amethyst in an unloaded chunk | Stays, and the chunk stays unloaded |
| Sphere reaching an unloaded chunk | The chunk stays unloaded, as in `TuningForkGameTests` |
| Client side | Covered by the `ServerLevel` check, so no test needed |

## Task 4: Multi-tick tests in the `shatter_note` environment

Each with blocks on the test's own column:

- Redstone: placing a redstone block beside the note block breaks the cluster.
- Left-click at 24 breaks it, through `BlockState.attack` with a mock player.
- Right-click at 23 breaks it, and right-click at 24 doesn't, because the note wraps to 0.
- Stone above: redstone power breaks nothing.
- A fresh copper hopper under the cluster ends up holding 2 shards.
- An oxidized copper hopper under the cluster holds none, with the 2 shards still on the ground.
- Note 24 still records music: `MusicGrowthFeature.tracker(level).isActiveNear` is true afterwards.

## Task 5: Measure

Add `worstCaseShatterFitsInATick`, following `worstCaseScanFitsInOneTick`, calling the search rather than breaking anything. Time three cases 500 times each and log the median, p99 and maximum:

- Every touched section holds a stale cluster palette entry, so nothing is skipped.
- A sphere full of clusters, which are collected but not broken.
- Untouched sections, which are skipped.

Multiply the worst median by 16 and by the 10 notes per second a note block can reach, averaged per tick. If that's over the spec's 1 ms per game tick, add its one-second cooldown, with a test. Otherwise record the numbers in the PR and add nothing. Assert only a loose ceiling, as the fork's test does, so a slow CI machine doesn't fail it.

## Task 6: Docs and version

- `gradle.properties`: `version=0.2.0`.
- `CHANGELOG.md`: a `0.2.0` entry.
- `README.md`, `docs/CURSEFORGE.md`, `docs/MODRINTH.md`: the shatter note, the three click and redstone behaviours, and the land-claim warning.
- `docs/CURSEFORGE.md`: the new jar's name and hash, after the final build.
- Update `build/listing/Patina-and-Chime-0.1.2-testing-guide.md` and its PDF as a new 0.2.0 guide with a step for this. They're not in git.

## Task 7: Verify

Run in order, stopping at the first failure:

```bash
./gradlew build runGameTest productionGameTest
```

```bash
./gradlew runClientGameTest productionClientGameTest
```

```bash
./gradlew runGameTest runClientGameTest -Pcompat=lithium
```

```bash
./gradlew runGameTest runClientGameTest -Pcompat=sodium,iris,continuity,modmenu,jade,appleskin,lithium,ferritecore,krypton
```

Check the exact compat names at the top of `build.gradle` first, and run the other mods tested for 0.1.2 too. Then open the PR with the `pr` skill's template and report what wasn't tested. Nobody will have heard it yet, so the three hand-test questions in the design go to the owner and the tester.
