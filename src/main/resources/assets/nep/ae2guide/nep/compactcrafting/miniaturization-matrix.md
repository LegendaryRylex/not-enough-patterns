---
navigation:
  title: Miniaturization Matrix
  icon: nep:miniaturization_matrix
  parent: nep/compactcrafting/index.md
  position: 52
item_ids:
  - nep:miniaturization_matrix
  - nep:miniaturization_pattern
---
# <Color id="gold">Miniaturization Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Miniaturization Matrix</Color>

  <ItemImage id="nep:miniaturization_matrix" scale="2"/>

  One block that runs a whole miniaturization craft. No projectors, no field, no stack of blocks to build: the Matrix takes the item form of everything a recipe's layers ask for, plus its catalyst, and hands the finished item back to the network.
</Column>

<Recipe id="nep:module_status/miniaturization_matrix"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="nep:miniaturization_matrix"/>
  <Block id="ae2:pattern_provider" z="1"/>
</GameScene>

* Put a Pattern Provider against any face and drop <ItemLink id="nep:miniaturization_pattern"/> patterns in it. The Matrix buffers the blocks and the catalyst as they arrive.
* The Matrix is an ME machine so it joins the network through its own grid connection, draws a small standing power cost, and returns finished items to the Provider on its own. It needs **no Import Card**.
* Miniaturization crafting has no energy cost of its own, so network power is all the Matrix ever burns.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The top readout names what is being crafted, how many jobs are queued, and how long the current one takes.
* The input buffer only accepts items a pending craft still needs, so loose items cannot clog it.
* **Clear Pending Recipes** drops every craft the Matrix still owes, cancels ingredients it has on request, and cancels the crafting jobs on the network that were waiting on it.
* **Empty Buffers** returns everything staged to your inventory.
* **Guide** opens this page, and is always the rightmost button.
* A Comparator beside the block reads the output buffer, empty for nothing staged up to a full 15.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Matrix must be on a powered ME network, not just connected to one.
* The Matrix takes up 15 channels by default, so it needs an ME Controller and a dense cable running to it. A network that cannot spare them leaves it offline.
* Check the recipe's field size is at or below the configured maximum. Above it, patterns are refused outright.
* A red *Missing ingredients* line means auto-request could not source something. Hover it for the list.
* Turn on Verbose Logging under Debug to have the Matrix log every pattern it refuses and why.