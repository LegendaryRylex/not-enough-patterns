---
navigation:
  title: Create
  icon: create:mechanical_crafter
  parent: nep/nep-index.md
  position: 60
---
# <Color id="aqua">Create</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Create</Color>

  <ItemImage id="create:mechanical_crafter" scale="2"/>

  Runs Create's machines as ME auto-crafting steps: Mechanical Crafter arrays, Deployers, Spouts and Sequenced Assembly lines, plus a Matrix that runs a whole assembly in one block.
</Column>

<Recipe id="nep:module_status/create"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="create:deployer" scale="0.5"/> Deploying | Deployer over a Depot | Andesite Crafting Pattern |
| <ItemImage id="create:spout" scale="0.5"/> Filling | Spout over a Depot | Andesite Crafting Pattern |
| <ItemImage id="create:mechanical_crafter" scale="0.5"/> Mechanical Crafting | Crafter array | Mechanical Crafting Pattern |
| <ItemImage id="nep:sequenced_assembly_controller" scale="0.5"/> Sequenced Assembly | Belt line with a Controller | Sequenced Assembly Pattern |
| <ItemImage id="nep:sequenced_assembly_matrix" scale="0.5"/> Sequenced Assembly | A single Matrix, no line | Sequenced Assembly Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:create_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Create patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:create_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
