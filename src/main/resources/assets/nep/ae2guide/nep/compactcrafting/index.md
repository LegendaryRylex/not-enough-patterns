---
navigation:
  title: Compact Crafting
  icon: nep:miniaturization_matrix
  parent: nep/nep-index.md
  position: 50
---
# <Color id="aqua">Compact Crafting</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Compact Crafting</Color>

  <ItemImage id="nep:miniaturization_matrix" scale="2"/>

  Runs miniaturization crafting as an ME auto-crafting step, on a Matrix that needs no field or through a Controller driving a projector field you built.
</Column>

<Recipe id="nep:module_status/compactcrafting"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="nep:miniaturization_controller" scale="0.5"/> Miniaturization Controller | A Controller driving a real field | Miniaturization Pattern |
| <ItemImage id="nep:miniaturization_matrix" scale="0.5"/> Miniaturization Matrix | A single Matrix, no field | Miniaturization Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:compact_crafting_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Compact Crafting patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:compact_crafting_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
