---
navigation:
  title: Infused Awakening Matrix
  icon: nep:infused_awakening_matrix
  parent: nep/mysticalagriculture/index.md
  position: 30
item_ids:
  - nep:infused_awakening_matrix
---
# <Color id="gold">Infused Awakening Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Infused Awakening Matrix</Color>

  <ItemImage id="nep:infused_awakening_matrix" scale="2"/>

  One block that runs both altars. No pedestals, no vessels, no ring to build: the Matrix stages the ingredients itself, banks essence in four tanks, and hands the finished item back to the network.
</Column>

<Recipe id="nep:module_status/infused_awakening_matrix"/>

> <Color id="yellow">The block reads its own state.</Color> The Matrix is an open frame with a faceted core turning inside it, visible through every face. The core drifts while the Matrix is idle, spins up and brightens as a craft runs, and flares when the craft lands. A stalled Matrix dims and judders in place, and its frame goes dark. Neighbouring Matrices are offset from one another, so a wall of them never turns in lockstep.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="nep:infused_awakening_matrix" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#88cc00">
    **Infused Awakening Matrix**, wired to the ME network
  </BlockAnnotation>
</GameScene>

* Put a Pattern Provider against any face and drop <ItemLink id="nep:infusion_pattern"/> or <ItemLink id="nep:awakening_pattern"/> patterns in it. The Matrix buffers the ingredients as they arrive.
* The Matrix is an ME machine: it joins the network through its own grid connection, draws a small standing power cost, and returns finished items to the Provider on its own. It needs **no Import Card**.
* It takes no FE. Infusion and awakening cost time, not power, and the only running cost is the ME network drain.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Four Essence Tanks</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The four bars on the right of the screen replace the Essence Vessels. Essence is an ordinary item, so a tank is really an item slot drawn as a bar: it takes one essence at a time, holds a stack of it, and is free for any other essence the moment it empties. Nothing is ever reserved, so the same four tanks serve every awakening recipe without being told in advance which essences it will want.

A tank holds <nep:ConfigValue name="infusedAwakeningMatrixTankCapacity"/>, where an Essence Vessel holds 40 and the priciest awakening recipe takes 40 for one craft. Raise it as far as 512 in the config to bank essence for a longer run.

| Getting essence in | How |
|---|---|
| Automatically | Leave Auto-Request on and the Matrix pulls or requests what a queued craft needs |
| By hand | Drop essence into the input buffer; anything an Essence Vessel would accept flows into a tank on its own |
| From a pipe or bus | Insert into any face, the same as any other ingredient |

> <Color id="yellow">Tanks fill through the input buffer.</Color> There is no separate port. Essence lands in the buffer like any item and moves into a tank on the next tick, which means hoppers, export buses and your own hand all work without a special setup.

> <Color id="yellow">Full tanks never refuse a pattern.</Color> Essence a tank cannot take waits in the input buffer as the item it is, and moves across as soon as a queued craft drains a tank, so the Matrix cannot lock itself out of a recipe.

> <Color id="yellow">Infusion recipes never touch the tanks.</Color> Only the essences an awakening recipe names in its vessels are banked. An infusion recipe that happens to use an essence as a pedestal ingredient takes it out of the input buffer as an ordinary item.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The top readout names what is being crafted and how many jobs are queued behind it, with the progress bar underneath.
* The input buffer only accepts items a pending craft still needs, plus essence up to the tanks' free space, so loose items cannot clog it.
* Each buffer slot holds up to 64 of any item, whatever that item normally stacks to, so a recipe wanting a pile of tools or potions still fits.
* Hover a tank for the essence it holds and how full it is.
* **Clear Pending Recipes** drops every craft the Matrix still owes, cancels ingredients it has on request, and cancels the crafting jobs on the network that were waiting on it.
* **Empty Buffers** returns everything staged, tanks included, to your inventory.
* **Guide** opens this page, and is always the rightmost button.
* A Comparator beside the block reads the output buffer, empty for nothing staged up to a full 15.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Crafting By Hand</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

An infusion or awakening recipe can also be started out of your own inventory, with no pattern and no Provider anywhere near it. Open the Matrix, find the recipe in the recipe viewer, and press its transfer button: every ingredient leaves your inventory in one go and the craft queues up like any other.

* It is all or nothing. Come up short and nothing moves: the button says so and the recipe marks the ingredients you are missing in red.
* Holding shift queues as many crafts as your inventory can pay for, up to sixty-four, trimmed to what the input buffer has room to stage.
* The finished item waits in the output buffer for you to take. Nothing goes to the network, and no crafting job is created.
* An awakening recipe's essence is pulled from your inventory too, and settles into the tanks by itself once it is inside.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Matrix must be on a powered ME network, not just connected to one.
* The Matrix takes up <nep:ConfigValue name="infusedAwakeningMatrixChannels"/> channels, and anything above 8 needs an ME Controller and a dense cable running to it. A network that cannot spare them leaves it offline.
* *Waiting on essence* means an awakening craft is staged but a tank is short. Feed that essence in through the input buffer; if it is already sitting there, the tanks are full and it moves across as soon as a queued craft drains one.
* A red *Missing ingredients* line means auto-request could not source something. Hover it for the list.
* Turn on Verbose Logging under Debug to have the Matrix log every pattern it refuses and why.