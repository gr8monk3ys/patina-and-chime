# Modrinth listing

Not submitted. Modrinth's [Content Rules](https://modrinth.com/legal/rules) don't allow a public project that is primarily or entirely AI output, and Claude wrote this mod's code, the scripts that draw its art and its listing text. A public listing depends on the steps below.

Slug: `patina-and-chime`. Type: mod. Loader: Fabric. Game version: 26.2. Client and server: both required. Licence: MIT. Dependency: Fabric API (required). Source: <https://github.com/gr8monk3ys/patina-and-chime>. Issues: <https://github.com/gr8monk3ys/patina-and-chime/issues>. Categories: game-mechanics, technology, utility.

## Before submitting

1. Ask Modrinth support whether hand-drawn art and a hand-written page are enough while the code stays AI-written. Don't start on the art until they answer.
2. Draw all 19 images by hand from scratch, without tracing the current ones, because the rules also cover images derived from AI output:
   - 12 block textures, 16×16: outside, top and inside for each of the 4 stages. Waxed blocks reuse these.
   - 6 item textures, 16×16: one hopper per stage, the tuning fork and verdigris.
   - The icon, 128×128.
3. Release a new version with that art, and take new gallery screenshots from it with `./gradlew productionClientGameTest`.
4. Write the page by hand. The summary should be short, unformatted and not repeat the title. [CURSEFORGE.md](CURSEFORGE.md) has the facts to work from.
5. Turn on the "Contains AI-generated content" disclosure.
