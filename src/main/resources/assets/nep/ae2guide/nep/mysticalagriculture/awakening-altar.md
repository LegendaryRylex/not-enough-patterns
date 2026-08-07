---
navigation:
  title: Awakening
  icon: mysticalagriculture:awakening_altar
  parent: nep/mysticalagriculture/index.md
  position: 20
item_ids:
  - nep:awakening_pattern
---
# <Color id="gold">Awakening</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Awakening</Color>

  <ItemImage id="mysticalagriculture:awakening_altar" scale="2"/>

  An Awakening Altar wants nine items spread across three kinds of block: the centre item in the altar, four ingredients on the Awakening Pedestals, and four stacks of essence in the Essence Vessels. All nine arrive in one push.
</Column>

<Recipe id="nep:module_status/awakening_altar"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="mysticalagriculture:awakening_altar" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#88cc00">
    **Awakening Altar**, ringed by four pedestals and four vessels
  </BlockAnnotation>
</GameScene>

* Build the altar with its four Awakening Pedestals and four Essence Vessels in the ring Mystical Agriculture expects. All eight are part of the machine.
* Put a Pattern Provider against any face of the altar and drop <ItemLink id="nep:awakening_pattern"/> patterns in it.
* Add an <ItemLink id="nep:import_card"/> to that Provider so the finished item comes back off the altar's output slot.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Essences Are Ingredients</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A recipe's four essences are part of the pattern like anything else, so the network sources them and the push fills the vessels itself. A vessel holds forty, which is exactly one craft of the priciest recipe, so the altar takes its essence a craft at a time.

The vessels must be **empty** when a pattern arrives. Essence left in them by hand blocks the push, because the altar would then consume more than the pattern asked the network for.

> <Color id="yellow">Tie the essences into auto-crafting.</Color> Air, Earth, Water and Fire essence are ordinary crop drops, so a farm feeding the ME system lets an awakening recipe be requested like any other item.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How a Push Works</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Provider hands over the centre item, the four pedestal ingredients and the four essence stacks together, then the altar starts itself. No redstone pulse is needed.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* In a Pattern Encoding Terminal, pick an awakening recipe from JEI and hit the transfer arrow. The pattern encodes as an <ItemLink id="nep:awakening_pattern"/>, essences and all.
* A plain processing pattern works too, as long as its inputs and output match exactly one awakening recipe.
* Gear recipes carry the old item's data onto the new one. The network sources a fresh, unenchanted item for the job, so an awakening requested through the ME system never reaches for the loaded one you are carrying.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A push is refused, and tried again later, whenever the altar cannot take a whole craft:

| Refusal | What to do |
|---|---|
| The altar already holds an input | Let the craft it is running finish |
| The output slot still holds a finished item | Collect it; an Import Card does this on its own |
| A pedestal or vessel is not empty | Clear the leftovers by hand |
| The pedestal or vessel count is wrong | Build four of each |