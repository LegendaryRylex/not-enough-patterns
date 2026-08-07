---
navigation:
  title: Infusion
  icon: mysticalagriculture:infusion_altar
  parent: nep/mysticalagriculture/index.md
  position: 10
item_ids:
  - nep:infusion_pattern
---
# <Color id="gold">Infusion</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Infusion</Color>

  <ItemImage id="mysticalagriculture:infusion_altar" scale="2"/>

  An Infusion Altar takes nine items at once: one in the altar and one on each of its eight pedestals. No Pattern Provider can reach a pedestal, so the whole craft is handed over in a single push instead.
</Column>

<Recipe id="nep:module_status/infusion_altar"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="mysticalagriculture:infusion_altar" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#88cc00">
    **Infusion Altar**, with its eight pedestals built around it
  </BlockAnnotation>
</GameScene>

* Build the altar and its eight pedestals exactly as Mystical Agriculture wants them. The pedestals are part of the machine, not decoration.
* Put a Pattern Provider against any face of the altar and drop <ItemLink id="nep:infusion_pattern"/> patterns in it.
* Add an <ItemLink id="nep:import_card"/> to that Provider. The altar leaves its result in its own output slot, and the card is what brings it back to the network.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How a Push Works</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Provider hands over all nine items in one go: the centre item goes into the altar, and one ingredient onto each pedestal. The altar then starts itself, so no redstone pulse and no right click is needed.

> <Color id="yellow">Infusion runs one craft at a time.</Color> The altar is a single-craft machine, so a stack of jobs queued on the network arrives one after another as each finishes. For volume, use the [Infused Awakening Matrix](/nep/mysticalagriculture/infused-awakening-matrix.md) instead.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* In a Pattern Encoding Terminal, pick an infusion recipe from JEI and hit the transfer arrow. The pattern encodes as an <ItemLink id="nep:infusion_pattern"/>.
* A plain processing pattern works too, as long as its inputs and output match exactly one infusion recipe. Where two recipes fit, NEP refuses rather than guessing, and the pattern has to be encoded from JEI.

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
| A pedestal is already holding an item | Clear the leftovers by hand |
| The pedestal count does not match the recipe | Build exactly as many pedestals as the recipe has ingredients |