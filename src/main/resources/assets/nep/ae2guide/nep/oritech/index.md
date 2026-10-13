---
navigation:
  title: Oritech
  icon: oritech:particle_accelerator
  parent: nep/nep-index.md
  position: 95
---
# <Color id="aqua">Oritech</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Oritech</Color>

  <ItemImage id="oritech:particle_accelerator" scale="2"/>

  Encodes the Particle Accelerator's collision recipes as patterns, and runs them on a real ring through the Particle Collision Controller, or with no ring at all in the Particle Collision Matrix. Oritech's other machines take items through ordinary inventories, so plain Processing Patterns already drive them.
</Column>

<Recipe id="nep:module_status/oritech"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="nep:particle_collision_controller" scale="0.5"/> Particle Collision | A Controller driving a Particle Accelerator | Particle Collision Pattern |
| <ItemImage id="nep:particle_collision_matrix" scale="0.5"/> Particle Collision | Particle Collision Matrix | Particle Collision Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:oritech_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Oritech patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:oritech_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
