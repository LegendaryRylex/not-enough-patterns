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

> <Color id="yellow">The block reads its own state.</Color> The Matrix is an open frame with a faceted core turning inside it, visible through every face. The core drifts while the Matrix is idle, spins up and brightens as a craft runs, and flares when the craft lands. A stalled Matrix dims and judders in place, and its frame goes dark. Neighbouring Matrices are offset from one another, so a wall of them never turns in lockstep.

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
* Each buffer slot holds up to 64 of any item, whatever that item normally stacks to, so a recipe wanting a pile of tools or potions still fits.
* **Clear Pending Recipes** drops every craft the Matrix still owes, cancels ingredients it has on request, and cancels the crafting jobs on the network that were waiting on it.
* **Empty Buffers** returns everything staged to your inventory.
* **Guide** opens this page, and is always the rightmost button.
* A Comparator beside the block reads the output buffer, empty for nothing staged up to a full 15.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Crafting By Hand</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A miniaturization recipe can also be started out of your own inventory, with no pattern and no Provider anywhere near it. Open the Matrix, find the recipe in the recipe viewer, and press its transfer button: every ingredient leaves your inventory in one go and the craft queues up like any other.

* It is all or nothing. Come up short and nothing moves: the button says so and the recipe marks the ingredients you are missing in red.
* Holding shift queues as many crafts as your inventory can pay for, up to sixty-four, trimmed to what the input buffer has room to stage.
* The finished item waits in the output buffer for you to take. Nothing goes to the network, and no crafting job is created.
* A recipe whose field is above the configured maximum is refused by hand exactly as a pattern for it would be.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Matrix must be on a powered ME network, not just connected to one.
* The Matrix takes up <nep:ConfigValue name="miniaturizationMatrixChannels"/> channels, and anything above 8 needs an ME Controller and a dense cable running to it. A network that cannot spare them leaves it offline.
* Check the recipe's field size is at or below the configured maximum. Above it, patterns are refused outright.
* A red *Missing ingredients* line means auto-request could not source something. Hover it for the list.
* Turn on Verbose Logging under Debug to have the Matrix log every pattern it refuses and why.