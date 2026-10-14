---
navigation:
  title: Bound Spirit Matrix
  icon: nep:bound_spirit_matrix
  parent: nep/occultism/index.md
  position: 10
item_ids:
  - nep:bound_spirit_matrix
categories:
  - Matrices
---
# <Color id="gold">Bound Spirit Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Bound Spirit Matrix</Color>

  <ItemImage id="nep:bound_spirit_matrix" scale="2"/>

  One block that does the work of spirit fire, a crusher spirit and a crystallizer spirit. No flames to throw items into, no spirit to summon and keep fed, and nothing left lying on the ground.
</Column>

<Recipe id="nep:module_status/bound_spirit_matrix"/>

> <Color id="yellow">The block reads its own state.</Color> The Matrix is an open frame with a faceted core turning inside it, visible through every face. The core drifts while the Matrix is idle, spins up and brightens as a craft runs, and flares when the craft lands. A stalled Matrix dims and judders in place, and its frame goes dark.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Obtaining</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Matrix is bound in a ritual on Occultism's afrit crafting pentacle, started with a bound Book of Binding for an afrit. The bowls hold an ME Molecular Assembler, four Matrix Circuitry, a Dimensional Matrix and two Spirit Attuned Crystals. The recipe viewer shows the ritual in full.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="nep:bound_spirit_matrix" z="0"/>
  <Block id="ae2:pattern_provider" z="1"/>
</GameScene>

* Put a Pattern Provider against any Matrix face and fill it with plain **Processing Patterns**: one input item, and the item the recipe makes as the output.
* Encode the output as the exact item the recipe produces. A crushing recipe that makes "any iron dust" makes one particular dust, and that is the one the pattern must name.
* The Matrix runs on ME network power alone. It needs no FE, no Import Card, and returns finished items to the Provider on its own.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Spirits</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The two slots on the right of the screen each take one [Spirit-Infused Book](/nep/occultism/spirit-infused-books.md): the top one a crusher, the bottom one a crystallizer.

* **Spirit fire needs no book.** It is always available, and every craft takes <nep:ConfigValue name="boundSpiritMatrixSpiritFireTicks"/> ticks.
* **Crushing and crystallizing need their book.** Without one, the Matrix turns those patterns away.
* **The tier is the spirit's.** A bound foliot, djinni, afrit or marid runs exactly the recipes that spirit could run in the world, at that spirit's speed and with that spirit's output, as Occultism's own tier settings define them. A stronger spirit's extra output goes back to the network along with the rest.
* **Several items at once.** A spirit that Occultism's settings let work several items in one go does the same here, finishing that many queued crafts in one craft's time.
* Swapping a book for a weaker one leaves any queued craft it can no longer run waiting until a strong enough book is back.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The readout names what is being made, how many jobs are queued, which kind of work is running and how long the craft takes.
* The three buttons clear the pending jobs, empty the buffers into your inventory, and open this page. **Guide** is always the rightmost of them.
* A Comparator beside the Matrix reads the output buffer, empty for nothing staged up to a full 15.
* Input slots only accept what the Matrix is actually short of, so a hopper can top it up without stuffing it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Crafting By Hand</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A spirit fire, crushing or crystallizing recipe can also be started out of your own inventory, with no pattern and no Provider anywhere near it. Open the Matrix, find the recipe in the recipe viewer, and press its transfer button: the input leaves your inventory and the craft queues up like any other.

* It is all or nothing. Come up short and nothing moves: the button says so and the recipe marks the ingredient you are missing in red.
* Holding shift queues as many crafts as your inventory can pay for, up to sixty-four, trimmed to what the input buffer has room to stage.
* The item in your hand is used first when it fits the recipe.
* Crushing and crystallizing still need their book in its slot.
* The finished items wait in the output buffer for you to take. Nothing goes to the network, and no crafting job is created.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **No network power.** The Matrix needs a powered ME network before it starts anything.
* **Not enough free network channels.** The Matrix takes up <nep:ConfigValue name="boundSpiritMatrixChannels"/> channels, and anything above 8 needs an ME Controller and a dense cable running to it. A network that cannot spare them leaves it offline.
* **Needs a crusher, or a crystallizer.** The pattern is a crushing or crystallizing recipe and no book for that job is in its slot.
* **Spirit too weak.** The bound spirit's tier is outside the range the recipe allows, or it makes fewer items than the pattern promises.
* **More than one input.** Every recipe this Matrix runs takes a single item, so a pattern with two different inputs is refused.
* **Output buffer full.** A finished craft with nowhere to go holds the machine at full progress until the buffer is cleared.
* **Two recipes for one item.** Only one recipe per result item can be queued at a time; a second pattern making the same item a different way is refused until the first finishes.
