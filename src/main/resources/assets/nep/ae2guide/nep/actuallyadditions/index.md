---
navigation:
  title: Actually Additions
  icon: actuallyadditions:empowerer
  parent: nep/nep-index.md
  position: 30
---
# <Color id="aqua">Actually Additions</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Actually Additions</Color>

  <ItemImage id="actuallyadditions:empowerer" scale="2"/>

  Runs empowering and Atomic Reconstruction as ME auto-crafting steps, on a real Empowerer with its Display Stands or on a Matrix that needs neither stands nor laser.
</Column>

<Recipe id="nep:module_status/actuallyadditions"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="actuallyadditions:empowerer" scale="0.5"/> Empowering | Empowerer with four Display Stands | Empowering Pattern |
| <ItemImage id="nep:atomic_empowering_matrix" scale="0.5"/> Empowering and Atomic Reconstruction | A single Matrix, no stands | Empowering or Atomic Reconstruction Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:actually_additions_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Actually Additions patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:actually_additions_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
