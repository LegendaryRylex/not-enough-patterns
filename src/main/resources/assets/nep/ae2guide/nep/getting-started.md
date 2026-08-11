---
navigation:
  title: Overview
  icon: ae2:pattern_provider
  parent: nep/nep-index.md
  position: 1
---
# <Color id="aqua">Overview</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Overview</Color>

  <ItemImage id="nep:import_card" scale="2"/>

  Every integration is one auto-crafting step. A Pattern Provider feeds the machine, then collects the result and marks the step done.
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What NEP Changes</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Point the Provider at the block each machine's page names and the auto-craft becomes system-driven. Sometimes this may be the machine itself, or the block it works over, like a Depot or a Crafting Core.
* Most modded machines leave their result in their output buffer, so the installing an <ItemLink id="nep:import_card"/> onto the Provider allows the system to collect it. Without one some crafts may never finish.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Matrices</Color>
</Column>

Most mods NEP has support for also have a Matrix machine.

These Matrices are a late-game "one-block solution" to that mods crafting methods.

Below is a list of all current Matrices available in the mod:

* <ItemImage id="nep:infused_awakening_matrix" scale="0.5"/> [Infused Awakening Matrix](/nep/mysticalagriculture/infused-awakening-matrix.md) (Mystical Agriculture)

<ItemImage id="minecraft:air" scale="0.5"/>


***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Configs for Pack Devs</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Integration settings live in `config/nep-server.toml`, or open Mods → Not Enough Patterns → Config. To vary them per world, drop a copy in that world's `serverconfig` folder and it overrides the shared file.

Every integration and Matrix have their own toggle. Disabling an integration stops it encoding patterns and stops Pattern Providers pushing to those machines. Patterns already in the world are left alone and resume working when the module is turned back on.

Each machine page lists the settings that belong to it, and a page whose module is switched off says so in red at the top. Turn on `Debug → Verbose Logging` to print every push and why it was accepted or rejected.

<Color id="green">Each mod has a Module Override master switch.</Color> Turn off Modules → Mystical Agriculture → Module Override and every Mystical Agriculture integration stops regardless of its own toggle; the same goes for Modules → Apothic Enchanting → Module Override.