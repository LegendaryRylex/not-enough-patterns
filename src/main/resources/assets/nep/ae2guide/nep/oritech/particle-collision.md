---
navigation:
  title: Particle Collision
  icon: oritech:accelerator_controller
  parent: nep/oritech/index.md
  position: 10
item_ids:
  - nep:particle_collision_pattern
---
# <Color id="gold">Particle Collision</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Particle Collision</Color>

  <ItemImage id="nep:particle_collision_pattern" scale="2"/>

  Encodes the Particle Accelerator's collision recipes as patterns, with the two colliding items as the ingredients.
</Column>

<Recipe id="nep:module_status/particle_collision"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Open a Pattern Encoding Terminal, pick a collision recipe in JEI and press **+**.
* Two items in, one item out. The pattern always promises a single result, because the accelerator adds one item to its output per collision whatever the recipe says.
* The pattern pushes its ingredients into adjacent inventories like a plain processing pattern.
* A collision needs one particle sped up before the other is released, which a Pattern Provider cannot time. Run these patterns through a [Particle Collision Controller](/nep/oritech/particle-collision-controller.md), or skip the ring with a [Particle Collision Matrix](/nep/oritech/particle-collision-matrix.md), which makes the recipe's full count.

<ItemImage id="minecraft:air" scale="0.5"/>

