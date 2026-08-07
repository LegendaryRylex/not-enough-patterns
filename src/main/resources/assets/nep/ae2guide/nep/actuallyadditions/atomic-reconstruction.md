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

  Encodes the Atomic Reconstructor's laser recipes as patterns. The recipes run in the <ItemLink id="nep:atomic_empowering_matrix"/>, not in the Reconstructor itself it has no item buffer.
</Column>

<Recipe id="nep:module_status/atomic_reconstruction"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Why Not The Reconstructor</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The real Atomic Reconstructor cannot be driven by a Pattern Provider, and no configuration changes that:

* Its input is whatever is **lying on the ground** touching the beam, and its output is a dropped item. There is nothing for a Provider to push into or pull out of.

Automating the block itself is still a job for other mods that add Ranged Collectors or hoppers. This module exists so the same recipes can be encoded and run in the Matrix instead.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Open a Pattern Encoding Terminal, pick a laser recipe in JEI and press **+**.
* One item in, one item out. Scale the pattern up and the Matrix runs that many crafts in a row, charging the recipe's energy for each one.
* Only a recipe id known to Actually Additions encodes. Building the same conversion by hand out of a plain processing pattern does not, because a Create recipe could claim the same one-in-one-out shape.
