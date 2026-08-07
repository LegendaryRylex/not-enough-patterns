# Not Enough Patterns

Not Enough Patterns is an addon mod for Applied Energistics 2 that enhances its autocrafting functions by creating patterns for machines that pattern providers can't usually handle on their own.

Pattern providers only cope with machines shaped like a furnace: items in, items out. This mod teaches them the rest. Every integration is one ordinary autocrafting step: encode a pattern, drop it in a provider, and the network drives the machine.

## Supported Mods

| Mod                  |Processes                                                   |
| -------------------- |----------------------------------------------------------- |
| Actually Additions   |Empowering                                                  |
| Apothic Enchanting   |Enchanting Infusion                                                    |
| Compact Crafting     |Miniaturization                                             |
| Create               |Mechanical Crafting, Deploying, Filling, Sequenced Assembly |
| Draconic Evolution   |Fusion Crafting                                             |
| Mystical Agriculture |Infusion, Awakening                                         |

Look a recipe up in JEI, hit the **+** button at a Pattern Encoding Terminal, and the right pattern comes back with the ingredients already worked out.

## Added Content

*   **Import Card** — Allows a pattern provider to collect its own results, tracked per pattern and per side, so it never takes anything that it shouldn't. Also fits the pattern providers from Advanced AE, ExtendedAE, and MEGA Cells.
*   **Controllers** — Manages a whole multi-step or multi-block process under one single block, moving items to where they need to go, then back into your system once the craft is finished.
*   **Matrices** — One-block late-game solutions that replace an entire multiblock process line, like Create's Sequenced Assembly.

## Airships

Every block NEP adds works aboard a **Sable** sub-level, which is what Create: Aeronautics builds its ships from. Screens stay open while the deck moves under you, the Sequenced Assembly Linker draws its overlay on the ship rather than where the sub-level is parked, and assembling a ship around a machine no longer duplicates what it was holding. Links have to stay on one sub-level: a controller cannot drive a machine that is on a different ship, or on the ground below it.

Sable is optional. NEP carries the small companion library it needs, so nothing changes on installs without it.

## Modpack Permission

You are free to include this mod in any Modpack on CurseForge or Modrinth.

## Documentation

Every machine has a full page in the **in-game AE2 Guide**, including setup diagrams, pattern encoding, troubleshooting, and its settings. Open the guide and look for _Not Enough Patterns_. That is the reference; this page is the summary.

## Configuration

NEP is built with pack devs in mind: `nep-server.toml` is fully modular. Every integration has its own section with an `enabled` toggle, and a master `modules.<mod>.allow_<mod>_module` as a master switch for that mod's integrations.

## Planned Mod Support

| Mod                    |Processes                                       |
| ---------------------- |----------------------------------------------- |
| Ars Nouveau            |Enchanting Core, Scribe's Table, Rituals(maybe) |
| Cooking for Blockheads |Kitchen Suite                                   |
| Enchanted              |Distillery, Kettle, Witch's Oven                |
| Excessive Utilities    |QED(maybe), Rainbow Generator Controller(maybe) |
| Farmer's Delight       |Cooking Pot, Skillet, Cutting Board             |
| Farming For Blockheads |Market Trades                                   |
| Iron's Spellbooks      |Alchemist Cauldron                              |
| Malum                  |Spirit Altar                                    |
| Mekanism               |Metallurgic Infuser                             |
| Occultism              |Rituals                                         |
| Oritech                |Particle Accelerator, Fluid Centrifuge, Cyber Station |
| Pneumaticcraft         |Pressure Chamber, Heat Crafting                 |