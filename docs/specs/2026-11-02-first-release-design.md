# Patina & Chime: first release design

Date: 2026-11-02. "Patina & Chime" is a working name; the Modrinth slug `patina-and-chime` was free on this date.

## Goal

A vanilla+ Fabric mod that gives copper and amethyst one shared identity, built on two rules taken from what vanilla already does:

- **Copper ages, and each age has its own job.** Vanilla's copper bulb dims from light 15 to 12, 8 and 4 as it oxidizes, and wax locks the stage.
- **Amethyst hears.** It chimes when walked on, and vanilla blocks relay sculk vibrations.

No option should be simply best. If one choice beats the others in every case, the design is wrong.

## Target

Minecraft 26.2, Fabric only, Java 25. Client and server both need the mod, since it adds blocks and items. MIT licence. All art is original: never copy or recolour Mojang's pixels, though looking at them for style and UV layout is fine.

## Scope of the first release

1. Copper hopper family
2. Verdigris
3. Tuning fork
4. Amethyst shards at the enchanting table
5. Music grows crystals
6. Amethyst note block instrument

Left out on purpose: amethyst block set, gutters, tuned wireless redstone, lightning coil, light rods, copper windows, amethyst bulb, copper hopper minecart, filtering (pcal43's Copper Hopper does that), and a config file.

## 1. Copper hopper family

Eight blocks: `copper_hopper`, `exposed_copper_hopper`, `weathered_copper_hopper`, `oxidized_copper_hopper`, and a `waxed_` version of each.

**Rule:** copper is cheaper than iron, so no stage may ever be faster than the vanilla hopper (8 ticks per item).

| Stage | Ticks per item | Pulls from container above | Picks up dropped items | Redstone |
|---|---|---|---|---|
| Fresh | 12 | yes | yes | Vanilla: power locks it |
| Exposed (Collector) | 16 | **no** | yes | Vanilla: power locks it |
| Weathered (Seized) | 24 | yes | yes | **Inverted: only runs while powered** |
| Oxidized (Crusted) | 32 | yes | **no** | Vanilla: power locks it |

Everything else matches the vanilla hopper: 5 slots, facing and pushing, comparator output, and accepting items pushed in by other hoppers at every stage.

- **Aging:** unwaxed stages age with vanilla's random-tick copper logic. Waxed stages never age and behave like their unwaxed stage. Honeycomb waxes, and an axe scrapes or removes wax, through the same vanilla and Fabric paths as other copper blocks.
- **Inventory must survive every stage change** (aging, scraping, waxing, wax removal, verdigris) with no items lost, dropped or duplicated.
- **Tooltips:** each item shows its stage's job. Unwaxed items that can still age also warn about it and say that wax keeps the job. An unwaxed hopper changes behaviour on its own, so this warning is how players avoid a farm quietly stalling.
- **Recipes:** `copper_hopper` uses the vanilla hopper shape with 5 copper ingots and a chest. Each stage plus a honeycomb crafts to its waxed version.
- **Block properties:** vanilla hopper strength and shape, copper sound, pickaxe required. Each block drops itself at its own stage, plus its contents.

## 2. Verdigris

Item `verdigris`: the green flakes scraped off old copper. It exists because the Weathered and Oxidized hopper jobs are not usable if the only way to reach them is waiting many hours.

- **Scraping drops it:** using an axe to remove a stage of oxidation drops 1 verdigris at the clicked face, except in creative. Removing wax drops nothing.
- **Using it ages copper:** right-click any unwaxed copper block that has a next stage to advance it one stage, consuming 1 (not in creative). This covers vanilla copper blocks and the copper hopper. It has no effect on waxed blocks or fully oxidized blocks. Doors and any other two-part blocks must change both parts together.
- **Recipes:** 1 copper ingot + 1 water bucket gives 2 verdigris, and the bucket is returned. 1 verdigris gives 1 cyan dye.

Scraping one stage and re-applying the flake nets out at zero, so there is no duplication loop.

## 3. Tuning fork

Item `tuning_fork`, the mod's copper and amethyst crossover. Stack size 1, no durability.

- **Recipe:** 3 copper ingots and 1 amethyst shard in a fork shape that differs from the spyglass (`C C` / ` S ` / ` C `).
- **Use:** right-click strikes it, with a 1 second cooldown. On the server, find the nearest `budding_amethyst` within a 32 block cube around the player. Skip chunk sections whose palette cannot contain it, so a scan stays cheap.
- **Feedback:** four sound events and matching action-bar messages, because sound alone excludes deaf and muted players.

| Nearest budding amethyst | Result |
|---|---|
| None in range | Dull ring |
| 17 to 32 blocks | Faint hum, low pitch |
| 9 to 16 blocks | Steady hum, middle pitch |
| 0 to 8 blocks | Strong hum, high pitch |

Sounds reuse vanilla audio by id, so no audio ships with the mod.

## 4. Amethyst shards at the enchanting table

The table speaks in unreadable runes, and amethyst hears it. The lapis slot also accepts amethyst shards.

- **Lapis:** unchanged. Costs 1/2/3 and shows one enchantment per offer.
- **Shards:** cost 2/4/6 and show every enchantment in each offer. Doubling the cost is what keeps lapis worth using.
- XP costs and the actual enchantment results do not change. Shards reveal offers; they don't change them.
- Shift-clicking moves shards into the slot, as with lapis.
- The hover text shows the shard cost, and an offer's button is only enabled when there are enough shards for it.
- **Reveal is computed on the client** using the same vanilla method and seed the menu already uses for its clue, so no custom network payload is needed. A modified client could read offers without paying, which is acceptable: free mods already do this.

## 5. Music grows crystals

Budding amethyst within 8 blocks (sphere, same as sculk sensor range) of active music has a 1 in 2 growth chance per random tick instead of vanilla's 1 in 5, which is 2.5 times as fast.

- **Active music:** a jukebox while its disc is playing, or a note block for 5 seconds after its last note.
- **Cost:** record music sources per level, and check them only inside the budding block's random tick. No scanning happens when music plays.
- **Feedback:** while boosted, budding amethyst shows a few note particles on its random tick, so players can tell it's working.

## 6. Amethyst note block instrument

A note block on a Block of Amethyst plays a pitched amethyst sound instead of harp. Mob heads on top still take priority, as with vanilla instruments. It uses a single-file sound event pointing at vanilla audio, so each note has a consistent base pitch. The pitch range is the normal 25 notes.

## Testing

- **Server behaviour:** Fabric GameTests for every rule above, written before the code where practical. Run with `./gradlew runGameTest`.
- **Client:** a client GameTest that places the content and takes screenshots for review, run during integration.
- **Needs a person:** whether the amethyst instrument sounds in tune, and whether the art feels vanilla.
- **Side by side:** check that pcal43's Copper Hopper and this mod can load together, since a recipe clash is possible.
