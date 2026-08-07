---
navigation:
  title: Filling
  icon: create:spout
  parent: nep/create/index.md
  position: 62
---
# <Color id="gold">Filling</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Filling</Color>

  <ItemImage id="create:spout" scale="2"/>

  Automate the Spout for `create:filling` recipes and Create's generic bucket and bottle filling. Like Deploying, the Depot is the machine: the base item is staged on it and a Spout two blocks above pours onto it.
</Column>

<Recipe id="nep:module_status/filling"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="create:depot" y="0"/>
  <Block id="create:spout" y="2"/>

  <BlockAnnotation y="2" color="#ffaa00">
    **Spout**, tank empty at the start
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#00ccff">
    **Depot**, and the Provider goes here
  </BlockAnnotation>
</GameScene>

* Spout two blocks above the Depot.
* The Provider goes on the <ItemLink id="create:depot"/>, not the Spout.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

An <ItemLink id="nep:andesite_crafting_pattern"/>. Look the recipe up in JEI, open a <ItemLink id="ae2:pattern_encoding_terminal"/>, and click the **+** on the recipe: the blank pattern comes back as an Andesite Crafting Pattern, naming the exact `create:filling` recipe to run. Inputs are the base item plus the fluid at the <Color id="red">exact</Color> amount the recipe uses.

A plain processing pattern with the same inputs and result still works, and is the only option for Create's generic bucket and bottle filling, which has no recipe to name.

> <Color id="yellow">The fluid amount must be exact.</Color> A different amount never matches, and the push is rejected.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Spout's tank must be empty when the craft starts.
* The base item and fluid amount must match the recipe exactly.