---
navigation:
  title: Spirit Infusion
  icon: malum:spirit_altar
  parent: nep/malum/index.md
  position: 10
item_ids:
  - nep:spirit_infusion_pattern
---
# <Color id="light_purple">Spirit Infusion</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="light_purple">Spirit Infusion</Color>

  <ItemImage id="malum:spirit_altar" scale="2"/>

  A Spirit Altar takes an item, the spirits that infuse it, and whatever else the recipe eats off nearby pedestals. No Pattern Provider can reach a spirit slot or a pedestal, so the whole craft is handed over in a single push instead.
</Column>

<Recipe id="nep:module_status/spirit_infusion"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="malum:spirit_altar" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#cc66ff">
    **Spirit Altar**, running the infusion itself
  </BlockAnnotation>
</GameScene>

* Put a Pattern Provider against any face of the altar and drop <ItemLink id="nep:spirit_infusion_pattern"/> patterns in it.
* Recipes that consume extra ingredients need free item pedestals or stands within the altar's own range, four blocks out and three up or down. One free pedestal per extra ingredient.
* No Import Card is needed here. The altar hands the finished item straight back to the Provider that ordered it, and only drops it on the floor when nothing around the altar will take it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How a Push Works</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Provider hands over the whole craft in one go: the item goes into the altar, the recipe's spirits into its spirit slots, and one extra ingredient onto each free pedestal. The altar picks the recipe up on its own, so no redstone pulse and no right click is needed.

The infusion then runs at the altar's own pace, which is Malum's pace: about fifteen seconds, cut down by every Spirit Altar accelerator built around it. A player crafting at the same altar by hand sees the result drop the way it always has.

> <Color id="yellow">Spirit Infusion runs one craft at a time.</Color> The altar holds a single item, so a stack of jobs queued on the network arrives one after another as each finishes. For volume, use the [Focused Spirit Matrix](/nep/malum/focused-spirit-matrix.md) instead.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* In a Pattern Encoding Terminal, pick a spirit infusion recipe from JEI and hit the transfer arrow. The pattern encodes as a <ItemLink id="nep:spirit_infusion_pattern"/>.
* A plain processing pattern works too, as long as its inputs and output match exactly one spirit infusion recipe. Where two recipes fit, NEP refuses rather than guessing, and the pattern has to be encoded from JEI.
* Recipes that carry the ingredient's own data over to the result, such as an infusion that keeps an item's enchantments, only encode for a plain ingredient carrying no data of its own. Anything else would hand the network something other than what the pattern promised.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A push is refused, and tried again later, whenever the altar cannot take a whole craft:

| Refusal | What to do |
|---|---|
| The altar is already holding an item | Let the infusion it is running finish |
| The altar is still holding spirits | Take them out; the altar only accepts a craft it can start clean |
| Not enough free pedestals in range | Build one free pedestal per extra ingredient, inside four blocks |
| The altar settled on a different recipe | Two recipes fit what was pushed; encode the pattern from JEI so it names the one you want |
