---
navigation:
  title: Imbuement
  icon: ars_nouveau:imbuement_chamber
  parent: nep/ars_nouveau/index.md
  position: 20
item_ids:
  - nep:imbuement_pattern
---
# <Color id="gold">Imbuement</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Imbuement</Color>

  <ItemImage id="ars_nouveau:imbuement_chamber" scale="2"/>

  An Imbuement Chamber holds one item and draws source into it over time. Most imbuement recipes are that item alone, and the rest also need an item on each Arcane Pedestal touching the chamber. Those pedestal items are never used up.
</Column>

<Recipe id="nep:module_status/imbuement_chamber"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="ars_nouveau:imbuement_chamber" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#88cc00">
    **Imbuement Chamber**, with Source Jars in reach
  </BlockAnnotation>
</GameScene>

* Put a Pattern Provider against any face of the chamber and drop <ItemLink id="nep:imbuement_pattern"/> patterns in it.
* Add an <ItemLink id="nep:import_card"/> to that Provider. The chamber leaves the finished item in its own slot, exactly where the reagent went in.
* Keep Source Jars close. The chamber only reaches two blocks for source, far less than the apparatus, and a push is refused when there is not enough within that.
* If a recipe calls for pedestal items, place Arcane Pedestals touching the chamber and put those items on them yourself. They stay there for every craft, so one chamber serves the recipes that share its pedestal items.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How a Push Works</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Provider puts the reagent into the chamber and nothing else. The chamber finds the recipe from the reagent and the pedestal items, drains the source it needs, and swaps the reagent for the result in place.

> <Color id="yellow">The result sits in the input slot.</Color> Nothing more can be pushed until it is taken out, which is what makes the Import Card worth adding rather than optional.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What Cannot Be Encoded</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The chamber is open to addons: several register imbuement recipes of their own, for charging a charm, scribing a codex in bulk, or writing a spell onto a scroll. All of those read their result off the item that went in, so there is no fixed output to name and no pattern to encode. They stay manual, and the chamber still runs them normally.

Recipes added by an addon as **plain** imbuement recipes are a different matter and work exactly like Ars Nouveau's own, with no extra support needed.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* In a Pattern Encoding Terminal, pick an imbuement recipe from JEI and hit the transfer arrow. The pattern encodes as an <ItemLink id="nep:imbuement_pattern"/> carrying the reagent only.
* A plain processing pattern works too, as long as its only input is the reagent and it matches exactly one imbuement recipe. One that also lists the pedestal items is refused, since those would leave the network for good.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Refusal | What to do |
|---|---|
| The chamber is already holding an item | Collect the finished item; an Import Card does this on its own |
| The pedestals do not hold this recipe's items | Put exactly the recipe's pedestal items on pedestals touching the chamber |
| Not enough source nearby | Add Source Jars within two blocks of the chamber |
