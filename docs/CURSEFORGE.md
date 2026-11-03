# CurseForge listing

Not submitted. Ready to submit: a tester other than the author has played 0.1.2, and 0.1.3 only adds a recipe. It needs the owner's CurseForge sign-in.

Name: Patina & Chime. Slug: `patina-and-chime`. Type: Mods. Loader: Fabric. Game version: 26.2. Java: 25. Client and server: both. Release type: Release. Licence: MIT. Required dependency: Fabric API 0.161.0 or newer. Source: <https://github.com/gr8monk3ys/patina-and-chime>. Issues: <https://github.com/gr8monk3ys/patina-and-chime/issues>. Categories: Technology, and Utility & QoL (check the names on the form).

File: `patina-and-chime-0.1.3+26.2.jar` from the `v0.1.3` release, sha256 `a6ed0e798dae5a514d9e770263b46bb42de320b256b0d11871d6f1d446ee7d8b`. Changelog: First release.

Avatar: the mod icon at 400×400. Gallery, from `./gradlew productionClientGameTest`: `0000_hoppers-from-the-side`, `0001_hoppers-from-above`, `0003_hopper-menu`, `0007_hopper-tooltip`, `0015_enchanting-3-shards-offer-3`, `0019_enchanting-lapis-offer-3`, `0020_music-growth-particles`.

## Summary

Copper hoppers that change job as they age, plus verdigris, a tuning fork, amethyst at the enchanting table and crystals that grow to music.

## Description

Patina & Chime gives copper and amethyst one shared idea, built from what vanilla already hints at. A copper bulb dims as it oxidizes, so copper ages into something different. Amethyst chimes when you walk on it, so amethyst hears.

### Copper hoppers

Copper is cheaper than iron, so a copper hopper is slower than an iron one at every stage. In exchange, each stage does a different job, and no stage is simply the best.

| Stage | Ticks per item | Pulls from the container above | Picks up dropped items | Redstone |
|---|---|---|---|---|
| Copper | 12 | Yes | Yes | Power locks it, like vanilla |
| Exposed | 16 | No | Yes | Power locks it, like vanilla |
| Weathered | 24 | Yes | Yes | Only runs while powered |
| Oxidized | 32 | Yes | No | Power locks it, like vanilla |

A vanilla hopper moves an item every 8 ticks. Copper hoppers age on their own. Wax one with honeycomb to keep its job, and scrape it with an axe to step it back. Its items stay put through every change.

Craft one like a vanilla hopper, with 5 copper ingots and a chest.

### Verdigris

Scraping oxidation off copper with an axe drops a flake of verdigris. Use a flake on unwaxed copper to age it one stage, so you can reach the job you want without waiting for hours. You can also craft it from a copper ingot and a water bucket, or turn it into cyan dye.

### Tuning fork

Strike it to listen for budding amethyst within 32 blocks. The sound and an action bar message tell you how close it is, so it works with the sound off too.

### Amethyst at the enchanting table

The lapis slot also takes amethyst shards. Shards cost double what lapis does, and they show every enchantment in each offer. They only reveal an offer, and never change it. Lapis works exactly as it does in vanilla.

### Music grows crystals

Budding amethyst within 8 blocks of a playing jukebox, or a note block that has just played, grows crystals 2.5 times as fast. Note particles show you when it's working.

### Amethyst note block

Put a note block on a Block of Amethyst to play a pitched amethyst sound.

### Shards from a Block of Amethyst

A Block of Amethyst crafts into 4 shards, the reverse of vanilla's recipe.

### Good to know

- Your client and any server you join both need the mod, along with Fabric API.
- It has been tested alongside Sodium, Iris, Lithium, FerriteCore, Krypton, Continuity, Mod Menu, JEI, REI, Jade, WTHIT, AppleSkin and Easy Magic. Easy Magic replaces the enchanting table's menu, so shards don't work on its tables, while lapis still does.
- The art isn't copied from Minecraft: scripts in the source repo draw it. The sounds reuse vanilla's audio.

### How it was made

I set the direction for this mod, including the theme, slower copper hoppers and a different job for each age. Claude, an AI coding tool, wrote the code, the scripts that draw the art and this page.
