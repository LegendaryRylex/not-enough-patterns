---
navigation:
  title: Mechanical Crafting
  icon: create:mechanical_crafter
  parent: nep/create/index.md
  position: 63
item_ids:
  - nep:mechanical_crafting_pattern
---
# <Color id="gold">Mechanical Crafting</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Mechanical Crafting</Color>

  <ItemImage id="create:mechanical_crafter" scale="2"/>

  Automate Create's Mechanical Crafters. Encode the recipe at a terminal like any other, or capture it from an array you have already built.
</Column>

<Recipe id="nep:module_status/mechanical_crafting"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="create:mechanical_crafter" x="0" y="1" z="0" p:facing="north" p:pointing="left"/>
  <Block id="create:mechanical_crafter" x="1" y="1" z="0" p:facing="north" p:pointing="down"/>
  <Block id="create:mechanical_crafter" x="0" y="0" z="0" p:facing="north" p:pointing="left"/>
  <Block id="create:mechanical_crafter" x="1" y="0" z="0" p:facing="north" p:pointing="down"/>
  <Block id="ae2:pattern_provider" x="0" y="2" z="0"/>

  <BlockAnnotation x="0" y="2" z="0" color="#00ccff">
    **Pattern Provider**, on any crafter in the array
  </BlockAnnotation>
  <BlockAnnotation x="1" y="0" z="0" color="#ffaa00">
    **Output crafter**, where every belt in the array leads
  </BlockAnnotation>
</GameScene>

* Build the array as Create wants it: one connected group of <ItemLink id="create:mechanical_crafter"/> whose arrows all lead to a single output.
* The array needs rotational speed. The Provider only stages ingredients, it does not turn anything.
* The Pattern Provider goes on any crafter in the array, not on the block the output drops into.
* No <ItemLink id="nep:import_card"/> is needed. The result leaves through the output crafter exactly as it does for a hand-fed array.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A <ItemLink id="nep:mechanical_crafting_pattern"/>. Look the recipe up in JEI, open a <ItemLink id="ae2:pattern_encoding_terminal"/>, and click the **+** on it: the blank pattern comes back as a Mechanical Crafting Pattern with the grid already arranged. This also covers vanilla recipes run in a crafter, with no <ItemLink id="ae2:molecular_assembler"/> needed.

Encoding by hand works too. Set the terminal to processing mode, fill in the ingredients and result, and encode: if they match a Mechanical Crafting recipe, the pattern comes back as a Mechanical Crafting Pattern regardless. NEP arranges the grid for you, up to 9x9:

* A recipe smaller than the array is placed near the output crafter to shorten item travel.
* Crafting is force started, so slot covers are optional.

> <Color id="yellow">A plain processing pattern is not accepted.</Color> The array ignores it and nothing is pushed, so any processing pattern you already had has to be re-encoded.

> <Color id="yellow">Vanilla recipes in a crafter</Color> only run with Create's `allowRegularCraftingInCrafter` enabled, the same as crafting by hand.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Capturing From a Crafter Array</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

An array you have already built can hand you the pattern directly, with no terminal and no JEI lookup. Fill a working crafter array with the recipe, using a <ItemLink id="create:crafter_slot_cover"/> for each empty cell. Then sneak and right click any crafter in it while holding a <ItemLink id="ae2:blank_pattern"/>. The layout and result bake into a <ItemLink id="nep:mechanical_crafting_pattern"/>, identical to one encoded at a terminal.

If it fails, the hotbar message says why:

| Message | Meaning |
|---|---|
| Invalid Mechanical Crafter arrangement | Not one connected array feeding a single output. |
| Every chained Mechanical Crafter needs an ingredient or a slot cover | A crafter is empty and uncovered. |
| No matching recipe for this arrangement | The filled layout is not a real recipe. |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The array must be turning. A crafter group at zero speed refuses every push.
* Every crafter in the array must be **empty**. One holding an ingredient from an earlier craft, or dropped in by hand, blocks the whole push until it is cleared.
* The array must still form one connected group feeding a single output. Break the chain and nothing is pushed.
* The array needs enough uncovered cells for the recipe. A <ItemLink id="create:crafter_slot_cover"/> takes its cell out of use, so covering more of a small array than the recipe leaves room for is refused.
* The pattern must be a <ItemLink id="nep:mechanical_crafting_pattern"/>. A plain processing pattern is never pushed.
* Turn on Verbose Logging under Debug to have the array log every pattern it refuses and why.