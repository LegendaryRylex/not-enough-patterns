---
navigation:
  title: Overview
  icon: ae2:pattern_provider
  parent: nep/nep-index.md
  position: 10
---
# <Color id="aqua">Overview</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Overview</Color>

  <ItemImage id="nep:import_card" scale="2"/>

  Every integration is one autocrafting step. A Pattern Provider feeds the machine, then collects the result and marks the step done.
</Column>

<Recipe id="nep:module_status/create"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What nep Changes</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Point the Provider at the machine for Mechanical Crafting, or at the <ItemLink id="create:depot"/> for Deploying and Filling.
* Create machines leave their result in the world, so the Provider needs a <ItemLink id="nep:import_card"/> to collect it. Without one the craft never finishes.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Config</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Every integration toggles on its own. Settings live in `config/nep-server.toml`, or open Mods → Not Enough Patterns → Config. To vary them per world, drop a copy in that world's `serverconfig` folder and it overrides the shared file.

Disabling an integration stops it encoding patterns and stops Pattern Providers pushing to those machines. Patterns already in the world are left alone and resume working when you turn it back on.

> <Color id="yellow">Modules → Create → Module Override is the master switch.</Color> Turn it off and every Create integration stops regardless of its own toggle. In the file it is `modules.create.allow_create_module`.

Each machine page lists the settings that belong to it, and a page whose module is switched off says so in red at the top. Turn on Debug → Verbose Logging to print every push and why it was accepted or rejected.
