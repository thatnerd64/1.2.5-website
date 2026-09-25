# Your Minecraft files go here

This folder is ignored by git. Nothing from Mojang is ever committed to this repository: every person who
builds the game supplies their own copy.

| Path | What | Where to find it |
|------|------|------------------|
| `input/minecraft.jar` | **Required.** Your Minecraft 1.2.5 client jar **with Minecraft Forge installed** (Forge 3.4.9 / FML 2.2 is what the Full Retro pack ships). | The Full Retro instance's `.minecraft/bin/minecraft.jar` |
| `input/resources/` | Optional, but needed for sound. Minecraft's sound and music folders (`sound/`, `newsound/`, `streaming/`, `music/`, `newmusic/`). | The instance's `.minecraft/resources/` |
| `input/jarmods/*.jar` or `*.zip` | Optional. Extra "jar mods" to layer over `minecraft.jar` (like the old "delete META-INF" install). Applied in alphabetical order. | — |

If your `minecraft.jar` is vanilla (no Forge), install Forge into it first, exactly as you would for the
desktop game: copy the contents of the Forge universal zip into the jar and delete `META-INF`.

Then build from the repository root:

```sh
./gradlew build
```
