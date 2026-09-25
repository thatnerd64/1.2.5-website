# Full Retro modpack content

Everything in this folder becomes the game's `.minecraft` directory in the browser:

| Path | Becomes |
|------|---------|
| `mods/` | `.minecraft/mods/` (jars, zips and class folders, loaded by FML exactly as on desktop) |
| `config/` | `.minecraft/config/`, the pack's mod configuration (block/item IDs etc.) |
| `buildcraft/`, `redpower/`, `profileImage/`, `mod_EE.props` | per-mod files in `.minecraft/` |
| `options.txt`, `servers.dat` | Minecraft's settings and server list |
| `resources/` | mod-supplied sounds, merged into `.minecraft/resources/` |

Configs edited in game are saved in the browser and take precedence over these files.

## Mods included

| Mod | File |
|-----|------|
| Minecraft Forge 3.4.9 + FML 2.2 | inside your `minecraft.jar` |
| BuildCraft 3.1.6 (Core, Builders, Energy, Factory, Silicon, Transport) | `buildcraft-client-3.1.6.25.jar` |
| Additional Pipes 3.2.0 | `additionalpipes-client-3.2.0.3.jar` |
| Logistics Pipes 0.4.5 | `LogisticsPipes-client-0.4.5.67.jar` |
| IndustrialCraft 2 1.97 | `industrialcraft-2-client_1.97.jar` |
| Forestry 1.4.8 | `forestry-client-A-1.4.8.4.jar` |
| Equivalent Exchange 2 1.4.6 | `EE2ClientV1.4.6.5.jar` |
| Mystcraft 0.9.1 | `mystcraft-client-1.2.5-0.9.1.02-Forge-3.3.8.152.jar` |
| Immibis Core, Tube Stuff, Dimensional Anchors | `immibis-core_*`, `tubestuff_*`, `zdimensional-anchor_*` |
| Portal Gun | `portalgun/` (class folder) |
| Block Helper, Mouse Tweaks, WorldEdit | `BlockHelper-*`, `MouseTweaks-*`, `WorldEdit.jar` |
| LumySkinPatch (skin fix, applied at build time) | `LumySkinPatch-*` |
| CraftPresence (Discord status; inert in a browser) | `CraftPresence-*` |

## Mods configured but missing

The pack's configs also cover these mods, but their jars are not in this repository and could not be downloaded
here: **RedPower 2**, **Thaumcraft 2**, **NotEnoughItems**/CodeChickenCore, **Railcraft**, EnderStorage,
ChickenChunks, Factorization, GraviSuite, IC2 Advanced Machines / Advanced Solar Panels / Charging Bench /
Compact Solars / Nuclear Control, Inventory Tweaks, Iron Chests, KeySprint, LaserMod, MineFactory Reloaded,
Modular Force Field System, Wireless Redstone, ArmorStatusHUD, StatusEffectHUD, Hidden Doors, Shelf,
Rei's Minimap and Single Player Commands.

To add one, put its 1.2.5 client jar/zip in `mods/` (the configs are already here) and rebuild. Mods that ship
as "jar mods" (edited `minecraft.jar` classes, e.g. Rei's Minimap, SPC, NEI's older versions) go in
`input/jarmods/` instead.
