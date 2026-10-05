---
navigation:
  title: Draconic Evolution
  icon: draconicevolution:crafting_core
  parent: nep/nep-index.md
  position: 70
---
# <Color id="aqua">Draconic Evolution</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Draconic Evolution</Color>

  <ItemImage id="draconicevolution:crafting_core" scale="2"/>

  Runs Fusion Crafting as an ME auto-crafting step, on a real Crafting Core with its Injectors or on a Matrix that replaces the whole multiblock.
</Column>

<Recipe id="nep:module_status/draconicevolution"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="draconicevolution:crafting_core" scale="0.5"/> Fusion Crafting | Crafting Core with Injectors | Fusion Crafting Pattern |
| <ItemImage id="nep:fusion_matrix" scale="0.5"/> Fusion Crafting | A single Matrix, no multiblock | Fusion Crafting Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:draconic_evolution_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Draconic Evolution patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:draconic_evolution_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
