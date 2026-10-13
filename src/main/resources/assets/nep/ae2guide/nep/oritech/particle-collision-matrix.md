---
navigation:
  title: Particle Collision Matrix
  icon: nep:particle_collision_matrix
  parent: nep/oritech/index.md
  position: 30
item_ids:
  - nep:particle_collision_matrix
---
# <Color id="aqua">Particle Collision Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Particle Collision Matrix</Color>

  <ItemImage id="nep:particle_collision_matrix" scale="2"/>

  One block that replaces the whole Particle Accelerator. It runs collision recipes with no ring, no motors and no particles in flight, paying in FE what the motors would have spent instead.
</Column>

<Recipe id="nep:module_status/particle_collision_matrix"/>

> <Color id="yellow">The block reads its own state.</Color> The Matrix is an open frame with a faceted core turning inside it, visible through every face. The core drifts while the Matrix is idle, spins up and brightens as a craft runs, and flares when the craft lands. A stalled Matrix dims and judders in place, and its frame goes dark. Neighbouring Matrices are offset from one another, so a wall of them never turns in lockstep.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="nep:particle_collision_matrix" z="0"/>
  <Block id="ae2:pattern_provider" z="1"/>
</GameScene>

* Put a Pattern Provider against any Matrix face and drop <ItemLink id="nep:particle_collision_pattern"/> patterns in it. The same patterns also run on a [Particle Collision Controller](/nep/oritech/particle-collision-controller.md).
* Provide the Matrix **FE** on any face.
* Short of FE, a craft draws on the ME network's own power instead, down to the last tenth the network keeps in reserve. The `Charge From ME Network` config turns that off.
* The Matrix is also an ME machine: It joins the network through its own grid connection, draws a small standing power cost, and returns finished items to the Provider on its own. It needs no **Import Card**.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How It Crafts</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **Cost comes from the recipe's speed.** A craft costs what Oritech's motors would have spent pushing a particle up to that recipe's collision speed, so it grows with the square of the speed: a speed 15000 recipe costs hundreds of times more than a speed 500 one. The `Energy Cost (%)` config scales it.
* **Time has a floor, not a ceiling.** A craft takes at least `Craft Ticks`. One costing more than the buffer holds still finishes, paying a buffer's worth each tick until it is covered, so the most expensive recipes run longer.
* **The whole result comes out.** A pattern always asks for one of the result, but the Matrix makes what the recipe names, and anything past the one the job wanted goes into the network.
* **Batched patterns are queued.** A pattern scaled to four crafts is four jobs, run one after another, each charged in full.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The readout names what is being made, how many jobs are queued, how much of the craft's energy has been paid, and the collision speed the recipe calls for.
* The three buttons clear the pending jobs, empty the buffers into your inventory, and open this page. **Guide** is always the rightmost of them.
* A Comparator beside the Matrix reads the output buffer, empty for nothing staged up to a full 15.
* Input slots only accept what the Matrix is actually short of, so a hopper can top it up without stuffing it.
* Each buffer slot holds up to 64 of any item, whatever that item normally stacks to.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Crafting By Hand</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A collision recipe can also be started out of your own inventory, with no pattern and no Provider anywhere near it. Open the Matrix, find the recipe in the recipe viewer, and press its transfer button: both ingredients leave your inventory in one go and the craft queues up like any other.

* It is all or nothing. Come up short and nothing moves: the button says so and the recipe marks the ingredients you are missing in red.
* Holding shift queues as many crafts as your inventory can pay for, up to sixty-four, trimmed to what the input buffer has room to stage.
* The finished items wait in the output buffer for you to take. Nothing goes to the network, and no crafting job is created.
* Energy comes from outside as always, so the Matrix still needs its FE and a powered network for a hand-started craft to move.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **No network power.** The Matrix needs a powered ME network before it starts anything, on top of its FE.
* **Not enough free network channels.** The Matrix takes up <nep:ConfigValue name="particleCollisionMatrixChannels"/> channels, and anything above 8 needs an ME Controller and a dense cable running to it. A network that cannot spare them leaves it offline.
* **No energy.** The FE buffer is empty. The craft resumes by itself once power returns.
* **Output buffer full.** A finished craft with nowhere to go holds the machine at full progress until the buffer is cleared.
* **Two recipes for one item.** Only one recipe per result item can be queued at a time; a second pattern making the same item a different way is refused until the first finishes.
