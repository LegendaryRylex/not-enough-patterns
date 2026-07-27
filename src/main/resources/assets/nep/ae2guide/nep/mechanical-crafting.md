---
navigation:
  title: Mechanical Crafting
  icon: create:mechanical_crafter
  parent: nep/nep-index.md
  position: 30
item_ids:
  - nep:mechanical_crafting_pattern
---
# <Color id="aqua">Mechanical Crafting</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Mechanical Crafting</Color>

  <ItemImage id="create:mechanical_crafter" scale="2"/>

  Automate Create's Mechanical Crafters. The recipe is captured by building it once in a real crafter array, not encoded at a terminal.
</Column>

<Recipe id="nep:module_status/mechanical_crafting"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding a Pattern</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Fill a working crafter array with the recipe, using a <ItemLink id="create:crafter_slot_cover"/> for each empty cell. Then sneak and right click any crafter in it while holding a <ItemLink id="ae2:blank_pattern"/>. The layout and result bake into a <ItemLink id="nep:mechanical_crafting_pattern"/>.

If it fails, the hotbar message says why:

| Message | Meaning |
|---|---|
| Invalid Mechanical Crafter arrangement | Not one connected array feeding a single output. |
| Every chained Mechanical Crafter needs an ingredient or a slot cover | A crafter is empty and uncovered. |
| No matching recipe for this arrangement | The filled layout is not a real recipe. |

<ItemImage id="minecraft:air" scale="0.25"/>

Load the pattern into a Provider on the array. The Provider fills the crafters; Create assembles the result.

> <Color id="yellow">The array still needs rotational speed.</Color> The Provider only stages ingredients.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Without a Crafter Array</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

You do not need a built array to make a pattern. Look the recipe up in JEI, open a <ItemLink id="ae2:pattern_encoding_terminal"/>, and click the **+** on it; the blank pattern comes back as a <ItemLink id="nep:mechanical_crafting_pattern"/> with the grid already arranged. This also covers vanilla recipes run in a crafter, with no <ItemLink id="ae2:molecular_assembler"/> needed.

Encoding by hand works too. Set the terminal to processing mode, fill in the ingredients and result, and encode: if they match a Mechanical Crafting recipe, the pattern comes back as a Mechanical Crafting Pattern regardless. nep arranges the grid for you, up to 9x9:

* A recipe smaller than the array is placed near the output crafter to shorten item travel.
* Crafting is force started, so slot covers are optional.

> <Color id="yellow">A plain processing pattern is not accepted.</Color> The array ignores it and nothing is pushed, so any processing pattern you already had has to be re-encoded.

> <Color id="yellow">Vanilla recipes in a crafter</Color> only run with Create's `allowRegularCraftingInCrafter` enabled, the same as crafting by hand.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Module Settings</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Under Modules → Create → Mechanical Crafting.

| Setting | Default | Effect |
|---|---|---|
| Enabled | true | Turns Mechanical Crafting on or off |
| Allow Vanilla Auto-Generated Recipes | true | Lets patterns resolve to vanilla shaped and shapeless recipes |

With the module off, crafter arrays stop accepting pushes. See [Overview](getting-started.md) for where the config lives and for the master switch.
