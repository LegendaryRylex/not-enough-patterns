---
navigation:
  title: Enchantment Infusion
  icon: apothic_enchanting:raven_enchanting_table
  parent: nep/apothic_enchanting/index.md
  position: 41
item_ids:
  - nep:enchanting_pattern
---
# <Color id="gold">Enchantment Infusion</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Enchantment Infusion</Color>

  <ItemImage id="apothic_enchanting:raven_enchanting_table" scale="2"/>

  Apothic Enchanting's infusion recipes need the enchanting table to sit inside a window of Eterna, Quanta, and Arcana. The <ItemLink id="apothic_enchanting:raven_enchanting_table"/> is the one table whose stats are set in its GUI rather than gathered from shelves, so NEP dials it to whatever the recipe asks for internally without touching player-set values.
</Column>

<Recipe id="nep:module_status/enchantment_infusion"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="ae2:pattern_provider" y="0"/>
  <Block id="apothic_enchanting:raven_enchanting_table" y="1"/>
</GameScene>

* Put a Pattern Provider against any face of the table. That is the whole build.
* No bookshelves, no Hellshelves, no Sightshelves. Shelves feed a table's stats, and this table's stats are set instead, so the ring around it does nothing for an automated craft.
* An <ItemLink id="nep:import_card"/> is also not required. The craft finishes in one tick and the result is handed straight back to the Provider that asked for it. If a Provider is not available, any other inventory touching the table takes it instead, and with no connected inventory at all the table drops it on top of itself rather than holding the job up.
* Only the Table of the Raven takes patterns as no other tables can be dialled to a recipe's requirements.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Infusion encodes as an <ItemLink id="nep:enchanting_pattern"/>. In a <ItemLink id="ae2:pattern_encoding_terminal"/>, pick an infusion recipe from JEI's Enchanting category and the input, the lapis and the experience are all filled in for you, and encoding turns the <ItemLink id="ae2:processing_pattern"/> into an Enchanting Pattern naming that one recipe.

Encoding a plain processing pattern by hand works too, as long as it carries the same three inputs. Encode it and NEP upgrades it to an Enchanting Pattern for whichever recipe those inputs pay for. The experience amount is the awkward part.

A named recipe never has to be guessed at. An Enchanting Pattern carries the recipe's id, so a table runs exactly the recipe you encoded even where a pack adds a second one taking the same input for the same result. A processing pattern is still accepted and is matched by its inputs instead.

<Color id="green">Only infusion automates.</Color> Ordinary enchanting hands you three offers drawn from your enchantment seed, so it makes no fixed result a pattern could ask for. Infusion recipes are the ones with a single certain output, and they are the ones NEP runs.

> <Color id="yellow">A recipe that makes more of its own ingredient cannot be requested.</Color> Apothic's built-in <ItemLink id="minecraft:echo_shard"/> infusion, for example, turns one shard into four, and AE2 hides the item you asked for from its own crafting calculation so that no recipe can feed itself. The plan therefore comes back partial with one shard missing however many you have stored, and the pattern is never pushed. This is a limit of AE2's crafting planner rather than of the table. There are other ways to automate recursive recipes, but they fall outside of NEP's scope.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Crafting Costs</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Infusion recipes are always the third offer at the table, so they cost **3 lapis**, but the amount of experience needed for each recipe changes per recipe. A pattern encodes the lapis cost, and pays the experience as a stored form of it instead, since no player is stood at the table to be charged.

Where a mod adds a fluid form of experience, that fluid is what a pattern asks for. NEP takes the first fluid carrying the `#c:experience` tag and charges it by the millibucket, and only falls back to <ItemLink id="minecraft:experience_bottle"/> when no such fluid is loaded. Either payment runs a craft, so a pattern encoded before an experience fluid arrived keeps working.

<ItemImage id="minecraft:air" scale="0.25"/>

<Row>
  ### <Color id="aqua">Experience costs:</Color>
</Row>

<ItemImage id="minecraft:air" scale="0.25"/>

| Required eterna | Levels charged | Experience | Fluid | Bottles |
|---|---|---|---|---|
| 0 | 3 | 26 | 520mB | 1 |
| 20 | 20 | 155 | 3100mB | 3 |
| 60 | 60 | 1091 | 21820mB | 16 |
| 80 | 80 | 1631 | 32620mB | 24 |

Doing it by hand can come out cheaper, because the table rolls a level for the offer and charges the lower of that roll and the cap. A pattern always pays the cap, so an automated infusion never costs less than the same craft at the table.

Both rates are settings. A bottle pays 70 points, ten times what throwing one is worth, so bottle counts stay small; a point costs 20mB, the rate the `#c:experience` tag documents.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Provider has to be on an Enchanting Table of the Raven. Nothing else accepts a push.
* The pattern has to carry the lapis and the experience. One that lists only the input and the result matches no recipe.
* The experience has to be one the table takes: a fluid carrying the `#c:experience` tag, or Bottles o' Enchanting. Another fluid is refused however much of it a pattern offers.
* Where two recipes fit the same inputs and result, a processing pattern is refused rather than guessed at. Encode it, or transfer from JEI, to get an Enchanting Pattern naming the one you mean.
* A plan that comes back as a partial one missing the recipe's own input is AE2 refusing to let a recipe feed itself, which is what the <ItemLink id="minecraft:echo_shard"/> infusion asks it to do. Nothing reaches the table, so nothing on it is at fault.
* Turn on Verbose Logging under Debug to have the table log why each push was refused.