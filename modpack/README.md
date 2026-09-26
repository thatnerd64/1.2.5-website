# Full Retro modpack content

Everything in this folder becomes the game's `.minecraft` directory in the browser:

| Path | Becomes |
|------|---------|
| `mods/` | `.minecraft/mods/` (jars, zips and class folders, loaded by FML exactly as on desktop) |
| `bin/` | `.minecraft/bin/`: extra classpath jars next to `minecraft.jar` (WorldEdit for SPC) |
| `jarmods/` | classes layered over `minecraft.jar` at build time (NEI, CodeChickenCore, SPC, FontFixer) |
| `config/` | `.minecraft/config/`, the pack's mod configuration (block/item IDs etc.) |
| `buildcraft/`, `redpower/`, `profileImage/`, `mod_EE.props` | per-mod files in `.minecraft/` |
| `options.txt`, `servers.dat` | Minecraft's settings and server list |
| `resources/` | mod-supplied sounds, merged into `.minecraft/resources/` |

Configs edited in game are saved in the browser and take precedence over these files.

## Mods included

46 FML/ModLoader mods load, plus these add-ons and jar mods.

| Group | Files (in `mods/` unless noted) |
|-------|------------------|
| Minecraft Forge 3.4.9 + FML 2.2 | inside your `minecraft.jar` |
| BuildCraft 3.1.6, Additional Pipes, Logistics Pipes | `buildcraft-client-*`, `additionalpipes-*`, `LogisticsPipes-*` |
| IndustrialCraft 2 1.97 and add-ons | `industrialcraft-2-*`, `AdvancedMachines_*`, `IC2NuclearControl_*`, `mod_chargingbench-*`, `mod_compactsolars-*`, `mod_zAdvancedSolarPanel_*`, `mod_zGraviSuite_*` |
| RedPower 2 (pr5b2) | `RedPower{Core,Control,Lighting,Logic,Machine,Wiring,World}-*` |
| Railcraft 5.3.3 | `Railcraft_Client_*` |
| Forestry 1.4.8 (+ IC2 crops) | `forestry-client-A-*`, `forestry-client-B-IC2Crops_*` |
| Equivalent Exchange 2 | `EE2ClientV*` |
| ChickenBones: ChickenChunks, EnderStorage, Wireless Redstone (CBE) | `ChickenChunks-*`, `EnderStorage-*`, `WR-CBE *` |
| NEI plugins | `NEIPatch.zip`, `NEI_{Buildcraft,Forestry,RailCraft,RedPower}Plugin*` |
| Immibis: Core, Tube Stuff, Dimensional Anchors | `immibis-core_*`, `tubestuff_*`, `zdimensional-anchor_*` |
| MFFS, Iron Chests, Shelf, Portal Gun | `mffs_*`, `mod_ironchests-*`, `shelf-*`, `portalgun/` |
| Inventory Tweaks, KeySprint, Mouse Tweaks, Block Helper | `InvTweaks-*`, `KeySprint.zip`, `MouseTweaks-*`, `BlockHelper-*` |
| Rei's Minimap | `[1.2.5]ReiMinimap_*` (settings in `rei_minimap/`) |
| WorldEdit (used by Single Player Commands) | `../bin/WorldEdit.jar` (settings in `sppcommands/`) |
| LumySkinPatch (applied at build time), CraftPresence (inert in a browser) | `LumySkinPatch-*`, `CraftPresence-*` |

Jar mods in `jarmods/`, layered over `minecraft.jar` in name order at build time: **CodeChickenCore 0.5.5**,
**FontFixer**, **NotEnoughItems 1.2.2.4** and **Single Player Commands 3.2.2**. They replace only vanilla
classes that Forge does not touch (FontRenderer, GuiContainer, EntityPlayerSP).

## Multiplayer version, and configured-but-absent mods

This is the mod set of Full Retro's **multiplayer** version, so a Full Retro server can be joined (through the
WebSocket relay, see [Multiplayer](../README.md#multiplayer)). The shared config folder also covers mods that only
the singleplayer edition ships (Thaumcraft 2, MineFactory Reloaded, Factorization, LaserMod, ArmorStatusHUD,
StatusEffectHUD, Hidden Doors); they are deliberately not included, and their configs are simply unused.

To add a mod, put its 1.2.5 client jar/zip in `mods/` (or a jar mod in `jarmods/`) and rebuild; for multiplayer the
server needs the matching server version. Extra jar mods
of your own can also go in `input/jarmods/`, applied after these.
