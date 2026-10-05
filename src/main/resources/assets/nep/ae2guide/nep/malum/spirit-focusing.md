---
navigation:
  title: Spirit Focusing
  icon: malum:spirit_crucible
  parent: nep/malum/index.md
  position: 15
item_ids:
  - nep:spirit_focusing_pattern
---
# <Color id="light_purple">Spirit Focusing</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="light_purple">Spirit Focusing</Color>

  <ItemImage id="malum:spirit_crucible" scale="2"/>

  A Spirit Crucible turns spirits into metal nodes, picking which node by the impetus you leave in it. The impetus is never eaten, so a pattern only ever pays in spirits.
</Column>

<Recipe id="nep:module_status/spirit_focusing"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="malum:spirit_crucible" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#cc66ff">
    **Spirit Crucible**, running the focusing itself
  </BlockAnnotation>
</GameScene>

* Put the impetus for the node you want into the crucible by hand, then put a Pattern Provider against the crucible and drop <ItemLink id="nep:spirit_focusing_pattern"/> patterns in it.
* The Provider must sit against the **crucible core**, the block the multiblock is named for, not one of its components. Nothing else in the structure carries the machine.
* No Import Card is needed. The crucible hands the finished node straight back to the Provider that ordered it, fortune bonuses included, and only drops them on the floor when nothing around it will take them.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Impetus Stays Yours</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Focusing has no item input. The impetus sitting in the crucible is what picks the recipe, and a craft cracks it a little rather than consuming it, so it never passes through the network at all.

That is why a pattern's whole cost is spirits, and why the impetus is left to you: an impetus that went in fresh and came back cracked is not the item the pattern promised, and the network would not accept it back. Slot it in, leave it there, and swap it when you want a different node.

> <Color id="yellow">One crucible focuses one node.</Color> A crucible holding an Iron Impetus takes iron patterns and waits on everything else. Give each node its own crucible, or use a <ItemLink id="nep:focused_spirit_matrix"/> with a <ItemLink id="nep:matrix_impetus"/>, which takes them all.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Augments Still Count</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The crucible runs the craft at its own pace, which is Malum's pace: every augment, catalyzer and tuning fork you have built around it applies exactly as it does for a player. Fortune bonuses come back to the Provider along with the node, so an Intricate Assembly pays out into the network rather than onto the floor.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* In a Pattern Encoding Terminal, pick a spirit focusing recipe from JEI and hit the transfer arrow. The pattern encodes as a <ItemLink id="nep:spirit_focusing_pattern"/>.
* A plain processing pattern works too. Every impetus focuses a different node, so the output alone is enough to name the recipe even though all twelve metals ask for the same two spirits.
* The pattern names the spirits and the node, never the impetus. Which crucible can run it is decided by what that crucible is holding.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A push is refused, and tried again later, whenever the crucible cannot take a whole craft:

| Refusal | What to do |
|---|---|
| The crucible is not holding the right impetus | Put the impetus that focuses this node into the crucible |
| The crucible is already focusing | Let the craft it is running finish |
| The crucible is still holding spirits | Take them out; it only accepts a craft it can start clean |
| The Provider is against a component block | Move it onto the crucible core |
