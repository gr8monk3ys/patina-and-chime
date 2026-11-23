# Use Mojang's mappings directly

The mod targets Minecraft 26.2 on Fabric Loom with Java 25. Minecraft 26.x ships unobfuscated, so the code uses Mojang's names directly and there is no remapping step or intermediary mappings. This ties the mod to one Minecraft minor version (`"minecraft": "~26.2"` in `fabric.mod.json`), which is accepted since each release already needs per-version testing.
