---
navigation:
  title: Atomic Empowering Matrix
  icon: nep:atomic_empowering_matrix
  parent: nep/actuallyadditions/index.md
  position: 33
item_ids:
  - nep:atomic_empowering_matrix
---
# <Color id="gold">Atomic Empowering Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Atomic Empowering Matrix</Color>

  <ItemImage id="nep:atomic_empowering_matrix" scale="2"/>

  One block that replaces both Actually Additions crafting machines. It runs empowering recipes with no Display Stands, and Atomic Reconstructor recipes with no laser; no loose items on the ground and no waiting for the next shot.
</Column>

<Recipe id="nep:module_status/atomic_empowering_matrix"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="nep:atomic_empowering_matrix" z="0"/>
  <Block id="ae2:pattern_provider" z="1"/>
</GameScene>

* Put a Pattern Provider against any Matrix face and drop both <ItemLink id="nep:empowering_pattern"/> or <ItemLink id="nep:atomic_reconstruction_pattern"/> patterns in it; The Matrix accepts both.
* Provide the Matrix **FE** on any face.
* The Matrix is also an ME machine: It joins the network through its own grid connection, draws a small standing power cost, and returns finished items to the Provider on its own. It needs no **Import Card**.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How It Crafts</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **Cost comes from the recipe.** An empowering craft costs what all four stands would have paid together; a reconstruction craft costs what the laser would have charged for that item. The `Energy Cost (%)` config scales both.
* **Time is set per craft type.** Empowering and reconstruction each have their own configured length, and the recipe's own duration is never used. Empowering recipes that take up to 500 ticks in the real multiblock take Empowering Craft Ticks here.
* **Batched patterns are queued.** A pattern scaled to four crafts is four jobs, run one after another, each charged in full.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The readout names what is being made, how many jobs are queued, and how much of the craft's energy has been paid.
* The three buttons clear the pending jobs, empty the buffers into your inventory, and open this page. **Guide** is always the rightmost of them.
* A Comparator beside the Matrix reads the output buffer, empty for nothing staged up to a full 15.
* Input slots only accept what the Matrix is actually short of, so a hopper can top it up without stuffing it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **No network power.** The Matrix needs a powered ME network before it starts anything, on top of its FE.
* **Not enough free network channels.** The Matrix takes up <nep:ConfigValue name="atomicEmpoweringMatrixChannels"/> channels, and anything above 8 needs an ME Controller and a dense cable running to it. A network that cannot spare them leaves it offline.
* **No energy.** The FE buffer is empty. The craft resumes by itself once power returns.
* **Output buffer full.** A finished craft with nowhere to go holds the machine at full progress until the buffer is cleared.
* **Two recipes for one item.** Only one recipe per result item can be queued at a time; a second pattern making the same item a different way is refused until the first finishes.
