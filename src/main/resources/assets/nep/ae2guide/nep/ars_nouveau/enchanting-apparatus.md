---
navigation:
  title: Enchanting
  icon: ars_nouveau:enchanting_apparatus
  parent: nep/ars_nouveau/index.md
  position: 10
item_ids:
  - nep:apparatus_pattern
---
# <Color id="gold">Enchanting</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Enchanting</Color>

  <ItemImage id="ars_nouveau:enchanting_apparatus" scale="2"/>

  An Enchanting Apparatus takes a reagent plus one item on each of its Arcane Pedestals. No Pattern Provider can reach a pedestal, so the whole craft is handed over in a single push instead.
</Column>

<Recipe id="nep:module_status/enchanting_apparatus"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="ars_nouveau:arcane_core" y="0"/>
  <Block id="ars_nouveau:enchanting_apparatus" y="1"/>
  <Block id="ae2:pattern_provider" y="2"/>

  <BlockAnnotation y="2" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="1" color="#88cc00">
    **Enchanting Apparatus**, with its Arcane Pedestals built around it
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#ffaa00">
    **Arcane Core**, which takes a Provider just as well
  </BlockAnnotation>
</GameScene>

* Build the apparatus and its pedestals as Ars Nouveau wants them, with its Arcane Core directly behind it. Patterns follow the same build rules as crafting by hand, so an apparatus with no core, or a core turned the wrong way, runs nothing. The pedestals are part of the machine, not decoration.
* Put a Pattern Provider against any face of the apparatus and drop <ItemLink id="nep:apparatus_pattern"/> patterns in it.
* The Arcane Core under the apparatus works as an attachment point too, and is usually the easier one to reach. A Provider against the core drives the apparatus it belongs to, so the apparatus itself stays free for pedestals and decoration.
* Add an <ItemLink id="nep:import_card"/> to that Provider. The apparatus leaves its result in its own slot, and the card is what brings it back to the network.
* Keep Source Jars within ten blocks. A push is refused outright when there is not enough source for the recipe.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How a Push Works</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Provider hands over every item in one go: the reagent goes into the apparatus, and one ingredient onto each pedestal. The apparatus then starts itself, so no right click is needed.

> <Color id="yellow">Recipes that keep an ingredient are refused.</Color> Anything on the `apparatus_not_consumed` tag, or with a crafting remainder, would come back out again, and a pattern has no way to say so. Those recipes stay manual.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What Cannot Be Encoded</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Two families derive their result from what the player puts in, so there is no fixed output to encode:

| Family | Why |
|---|---|
| Prestidigitation | The result depends on what went in |
| Spell write | The result carries the spell being written |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Enchantments and Armour Upgrades</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

One enchantment or armour upgrade recipe covers many items, so the pattern is encoded for the reagent JEI is showing when you hit the transfer arrow.

* **Enchantments** take exactly the book the pattern names: a plain Book for the first level, and the book one level below for every level after it. The result is that book one level higher. Encode one pattern per level and AE2 chains them, so asking for a third level crafts the second from the first on the way. A book carrying other enchantments never matches, so a valuable book is never spent.
* **Armour upgrades** take any piece of that armour at the tier below, whatever threads, enchantments or name it carries. The upgraded piece comes back with all of it, and the crafting job counts it as the piece the pattern promised.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* In a Pattern Encoding Terminal, pick an apparatus recipe from JEI and hit the transfer arrow. The pattern encodes as an <ItemLink id="nep:apparatus_pattern"/>.
* A plain processing pattern works too, as long as its inputs and output match exactly one apparatus recipe. Where two recipes fit, NEP refuses rather than guessing.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A push is refused, and tried again later, whenever the apparatus cannot take a whole craft:

| Refusal | What to do |
|---|---|
| No Arcane Core behind the apparatus | Place one on the side opposite the way the apparatus faces |
| The Arcane Core faces the wrong way | Break it and place it again while looking toward the apparatus |
| The apparatus is already crafting | Let the craft it is running finish |
| The apparatus still holds a finished item | Collect it; an Import Card does this on its own |
| A pedestal is already holding an item | Clear the leftovers by hand |
| Fewer pedestals than the recipe needs | Build one pedestal per ingredient |
| Not enough source nearby | Add Source Jars within ten blocks |

A Provider on an Arcane Core that does nothing at all is a different problem: the core has no apparatus to drive. The apparatus has to be the block sitting on the face the core was built against, which for a normal build is the one directly above it.
