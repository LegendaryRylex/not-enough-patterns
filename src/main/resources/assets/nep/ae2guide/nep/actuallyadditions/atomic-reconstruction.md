---
navigation:
  title: Atomic Reconstruction
  icon: actuallyadditions:atomic_reconstructor
  parent: nep/actuallyadditions/index.md
  position: 31
item_ids:
  - nep:atomic_reconstruction_pattern
---
# <Color id="gold">Atomic Reconstruction</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Atomic Reconstruction</Color>

  <ItemImage id="nep:atomic_reconstruction_pattern" scale="2"/>

  Encodes the Atomic Reconstructor's laser recipes as patterns. They run in the <ItemLink id="nep:atomic_empowering_matrix"/>, or push their ingredients out like a plain processing pattern to feed the real Reconstructor.
</Column>

<Recipe id="nep:module_status/atomic_reconstruction"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Matrix Or The Real Thing</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The same pattern drives both machines:

* **In the Matrix.** Put the pattern in a Provider on any Matrix face. Ingredients, energy and outputs are all handled inside the block.
* **At the real Reconstructor.** The Reconstructor has no item buffer. Its input is whatever is **lying on the ground** touching the beam, and its output is a dropped item. So the Provider cannot push into the block itself: aim it at a dropper facing the beam instead, and bring the drops back into the network with a Ranged Collector or hopper feeding an ME Interface.

A Provider face holding the Matrix always uses the Matrix. Every other adjacent inventory receives the raw ingredients like any processing pattern push, so give these patterns their own Provider if it also touches storage.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Open a Pattern Encoding Terminal, pick a laser recipe in JEI and press **+**.
* One item in, one item out. Scale the pattern up and the Matrix runs that many crafts in a row, charging the recipe's energy for each one.
* Only a recipe id known to Actually Additions encodes. Building the same conversion by hand out of a plain processing pattern does not, because a Create recipe could claim the same one-in-one-out shape.
