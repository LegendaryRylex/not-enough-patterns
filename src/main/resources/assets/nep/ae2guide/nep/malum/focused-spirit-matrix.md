---
navigation:
  title: Focused Spirit Matrix
  icon: nep:focused_spirit_matrix
  parent: nep/malum/index.md
  position: 20
item_ids:
  - nep:focused_spirit_matrix
---
# <Color id="blue">Focused Spirit Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="blue">Focused Spirit Matrix</Color>

  <ItemImage id="nep:focused_spirit_matrix" scale="2"/>

  One block that channels spirits into anything Malum will take them. Infusion without an altar, focusing without a crucible: the Matrix stages every ingredient in its own buffer and hands the finished item back to the network.
</Column>

<Recipe id="nep:module_status/focused_spirit_matrix"/>

> <Color id="yellow">The block reads its own state.</Color> The Matrix is an open frame with a faceted core turning inside it, visible through every face. The core drifts while the Matrix is idle, spins up and brightens as a craft runs, and flares when the craft lands. A stalled Matrix dims and judders in place, and its frame goes dark. Neighbouring Matrices are offset from one another, so a wall of them never turns in lockstep.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="nep:focused_spirit_matrix" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#3e7fe0">
    **Focused Spirit Matrix**, wired to the ME network
  </BlockAnnotation>
</GameScene>

* Put a Pattern Provider against any face and drop <ItemLink id="nep:spirit_infusion_pattern"/>, <ItemLink id="nep:spirit_focusing_pattern"/> or <ItemLink id="nep:runeworking_pattern"/> patterns in it. The Matrix buffers the ingredients as they arrive.
* The Matrix is an ME machine: it joins the network through its own grid connection, draws a small standing power cost, and returns finished items to the Provider on its own. It needs **no Import Card**.
* It takes no FE. Spirit infusion costs time, not power, and the only running cost is the ME network drain.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">No Spirits, No Pedestals</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A Spirit Altar splits a recipe three ways: the item goes in the altar, the spirits go in its spirit slots, and every extra ingredient waits on its own pedestal. The Matrix keeps all three inside itself, so a recipe with six spirits and four extra ingredients needs nothing built around the block.

The screen is that split, laid out left to right: the **input buffer** on the left for the item and its extra ingredients, the **upgrade column** in the middle, and the **spirit bank** on the right.

That is also the difference in throughput. The altar runs one craft at a time and asks for room around it; the Matrix runs one craft at a time in a block on its own, so a wall of them costs nothing but the space they stand in.

> <Color id="yellow">The Matrix shares its patterns with the real machines.</Color> The same <ItemLink id="nep:spirit_infusion_pattern"/> works in an altar or a Matrix, the same <ItemLink id="nep:spirit_focusing_pattern"/> works in a crucible or a Matrix, and the same <ItemLink id="nep:runeworking_pattern"/> works on a workbench or a Matrix, so moving a recipe between them is a matter of moving the pattern.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Spirit Bank</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The nine slots on the right are one per spirit, always in the same place, with Umbral in the middle. A slot takes its own spirit and nothing else, so a hopper cannot scatter one kind across the grid and a glance tells you what the Matrix is short of.

|  |  |  |
|---|---|---|
| <ItemImage id="malum:sacred_spirit" scale="0.5"/> Sacred | <ItemImage id="malum:aerial_spirit" scale="0.5"/> Aerial | <ItemImage id="malum:wicked_spirit" scale="0.5"/> Wicked |
| <ItemImage id="malum:aqueous_spirit" scale="0.5"/> Aqueous | <ItemImage id="malum:umbral_spirit" scale="0.5"/> Umbral | <ItemImage id="malum:infernal_spirit" scale="0.5"/> Infernal |
| <ItemImage id="malum:arcane_spirit" scale="0.5"/> Arcane | <ItemImage id="malum:earthen_spirit" scale="0.5"/> Earthen | <ItemImage id="malum:eldritch_spirit" scale="0.5"/> Eldritch |

**The bank is network storage.** What it holds shows up in your terminal and counts toward a craft, so a recipe wanting eight Arcane goes ahead on the Matrix's own stock even with not one shard in a drive. The spirits leave the bank as the job starts and come straight back with the pattern.

> <Color id="yellow">Spirits a queued craft owes stop counting.</Color> The moment a pattern lands, its spirits are spoken for and drop out of what the network sees, so queueing craft after craft can never spend the same shard twice. A bank of sixty-four Arcane is worth exactly eight jobs of eight, and the ninth waits for more.

**Restock Spirits** keeps all nine slots topped up from the rest of the network, crafting a spirit where a pattern exists. A slot the bank has run dry of fills itself back up, so a Matrix left running never quietly stops for want of a shard. The Matrix never restocks out of its own bank, so this only ever moves spirits toward the machine. Leave it off and the bank is yours to fill.

> <Color id="yellow">A spirit from an addon still works.</Color> Malum's own nine fill the grid exactly, so a spirit some other mod adds has no slot of its own. It goes through the input buffer as an ordinary ingredient instead: the recipe runs as normal, that spirit just does not get the bank's place in network storage.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Obelisks</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Runewood Obelisks accelerate a Spirit Altar built beside them, and they do the same job here without being built at all. Drop them in the obelisk slot, the one sitting on its own above the pair, and the Matrix infuses faster.

| Obelisk | What a full slot does |
|---|---|
| <ItemImage id="malum:runewood_obelisk" scale="0.5"/> Runewood Obelisk | Drives the crafting time down to a floor, 5 ticks by default |

A bare Matrix infuses in 90 ticks. The slot takes up to thirty-two obelisks, and a partly filled slot scales in a straight line from there down to the floor, so the step each obelisk is worth follows whatever the slot size is set to. Hover the slot for what the obelisks in it are worth right now.

> <Color id="yellow">The Brilliant Obelisk is not accepted.</Color> Only the Runewood Obelisk accelerates an altar in Malum; a Brilliant Obelisk extends the reach of totem rites and does nothing for an infusion.

> <Color id="yellow">Obelisks are never consumed.</Color> They sit in the slot as an upgrade and come back out whenever you want them, or drop with everything else when the Matrix is broken.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Focusing</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The same block runs spirit focusing, the Spirit Crucible's job of turning spirits into metal nodes. Focusing has no item input at all, so the recipe's spirits come straight out of the bank and nothing touches the input buffer.

Two slots under the obelisk make it work, and they are the whole of the difference:

| Slot | What goes in it |
|---|---|
| <ItemImage id="nep:matrix_catalyzer" scale="0.5"/> Matrix Catalyzer | Up to four. A full slot cuts focusing time by 75% and carries three of the crucible's augment effects |
| <ItemImage id="nep:matrix_impetus" scale="0.5"/> Matrix Impetus | One. Unlocks every focusing recipe there is |

A <ItemLink id="nep:matrix_impetus"/> stands in for every impetus Malum has, so one Matrix focuses every node instead of one Matrix per metal. It is built from a <ItemImage id="malum:wind_nucleus" scale="0.5"/> Wind Nucleus, which only a real Spirit Crucible makes, so the crucible gets built and run before any of this is automated. Its infusion also asks for void salts and umbral spirits, so the recipe stays out of sight until you have uncovered the Secrets of the Void.

The catalyzer is where focusing pays off. Malum's focusing recipes run from 300 ticks to 2700, far longer than an infusion, and a full slot of catalyzers takes the usual 900 tick node down to about 225.

Catalyzers also carry three effects a real crucible buys with augments, each scaling with how full the slot is:

| Effect | What a full slot is worth |
|---|---|
| Restoration | 50% chance a craft mends a hundredth of the impetus' durability |
| Chain focusing | 25% chance the next focusing craft starts all but finished |
| Fortune | 25% chance the craft yields a second copy of its output |

Each figure is the cap a full slot reaches, so one catalyzer out of four is worth a quarter of it. Shrink the slot in the config and every catalyzer counts for more, because the caps do not move.

> <Color id="yellow">Bonus output is returned to the network.</Color> A crafting job takes the count it ordered and the surplus lands in storage, so fortune quietly stocks your drives rather than confusing the job that triggered it.

A chained craft still claims its own ingredients and its own durability. What it skips is the waiting, not the cost. **Catalyzer Chain Focusing Chance** is capped at 99% in the config to avoid infinite-looping crafts: at 100% every craft would chain into the next one forever.

Each focusing craft cracks the Matrix Impetus the way a real crucible would, and restoration is what stretches one out. Turn off **Consume Impetus Durability** in the config and the impetus never wears, which also leaves restoration with nothing to mend.

It carries <nep:ConfigValue name="matrixImpetusDurability"/> durability, double what any of Malum's own impetuses do, on the grounds that it does the work of all of them. **Impetus Durability** in the config moves that figure, and it moves it for every impetus already made rather than only for the next one crafted.

> <Color id="yellow">Focusing can be switched off for the Matrix alone.</Color> Turn **Spirit Focusing Enabled** off under the Matrix's focusing settings and it becomes an infusion machine: both the catalyzer and the impetus slots close, and a focusing pattern pushed at it is refused outright so the network looks elsewhere. That says nothing about the Spirit Focusing module, so a pattern provider against a real Spirit Crucible keeps running focusing recipes as it always did.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">When the Impetus Runs Out</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The craft that spends the last of the durability still finishes, and what is left in the slot afterwards is a <ItemLink id="nep:fractured_matrix_impetus"/>. The Matrix refuses focusing work from that moment, exactly as it would with an empty slot, so a job never sits waiting on a stone that cannot do it. Restoration rolls before the fracture, so a lucky catalyzer bank can pull an impetus back from its last point of durability.

A fractured impetus cannot go back in the slot. It goes to a <ItemLink id="malum:repair_pylon"/> instead, as spirit repair, which puts it back together whole:

| What the repair wants | Where it goes |
|---|---|
| <ItemImage id="nep:fractured_matrix_impetus" scale="0.5"/> Fractured Matrix Impetus | On an item pedestal or stand near the pylon |
| <ItemImage id="nep:matrix_circuitry" scale="0.5"/> Matrix Circuitry | One, in the pylon |
| <ItemImage id="malum:umbral_spirit" scale="0.5"/> Umbral | One, in the pylon |
| <ItemImage id="malum:arcane_spirit" scale="0.5"/> Arcane | Two, in the pylon |
| <ItemImage id="malum:eldritch_spirit" scale="0.5"/> Eldritch | Two, in the pylon |

> <Color id="yellow">Repair returns a full impetus, not a patched one.</Color> Spirit repair hands back a brand new <ItemLink id="nep:matrix_impetus"/> at whatever Impetus Durability is set to, so raising that figure upgrades the impetuses you already own the next time they come apart. The recipe only shows in the recipe viewer while **Consume Impetus Durability** is on, since nothing can fracture an impetus with it off.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Crafting By Hand</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

An infusion, a shaping or a focusing can also be started out of your own inventory, with no pattern and no Provider anywhere near it. Open the Matrix, find the recipe in the recipe viewer, and press its transfer button: every ingredient leaves your inventory in one go and the craft queues up like any other.

* It is all or nothing. Come up short and nothing moves: the button says so and the recipe marks the ingredients you are missing in red.
* Holding shift queues as many crafts as your inventory can pay for, up to sixty-four, trimmed to what the buffers have room to stage.
* Spirits come out of your inventory too, and land in the bank rather than the input buffer. The bank's own stock is not spent on a hand-started craft, so what you hand over is exactly what the recipe asks for.
* The finished item waits in the output buffer for you to take. Nothing goes to the network, and no crafting job is created.
* Focusing by hand still runs on the Matrix's own impetus and still cracks it, so the impetus slot has to hold a whole one before the button will do anything.
* Energy comes from outside as always, so the Matrix still has to be powered and on a network for a hand-started craft to move.

> <Color id="yellow">One result at a time.</Color> A hand-started craft and a pattern for the same result cannot queue together: whichever arrives second is refused, so the Matrix never has to guess who a finished item belongs to.

> <Color id="yellow">Infusions that keep an item's data spend the one you are holding.</Color> A recipe that carries enchantments, durability or other component data across takes the matching item out of your hand rather than the first one it finds in your inventory, so hold the tool you mean to infuse. The recipe viewer names it on the transfer button before you press it. One of those queues at a time, however many the button offers, since each craft has its own item to carry across.

Turn **Manual Crafting** off under Machines in the config to leave the Matrix crafting only what a Pattern Provider pushes to it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The top readout names what is being made and how many jobs are queued behind it, with the progress bar underneath, and whichever upgrade is driving the craft alongside the time it buys. Obelisks show while infusing, catalyzers while focusing.
* The input buffer only accepts items a pending craft still needs, so loose items cannot clog it. Spirits never go here; they belong in the bank.
* Each buffer slot holds up to 64 of any item, whatever that item normally stacks to, so a recipe wanting a pile of tools or potions still fits.
* **Restock Spirits** toggles the bank between filling itself from the network and being stocked by hand.
* **Clear Pending Recipes** drops every craft the Matrix still owes, cancels ingredients it has on request, and cancels the crafting jobs on the network that were waiting on it.
* **Empty Buffers** returns everything staged, the spirit bank included, to your inventory. Obelisks, catalyzers and the impetus stay where they are.
* **Guide** opens this page, and is always the rightmost button.
* A Comparator beside the block reads the output buffer, empty for nothing staged up to a full 15.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Matrix must be on a powered ME network, not just connected to one.
* The Matrix takes up <nep:ConfigValue name="focusedSpiritMatrixChannels"/> channels, so it needs an ME Controller and a dense cable running to it. A network that cannot spare them leaves it offline.
* *Waiting on ingredients* means the job is staged but something has not arrived yet. It starts on its own the moment the buffer is complete.
* A red *Missing ingredients* line means auto-request could not source something. Hover it for the list.
* *No Matrix Impetus* means a focusing pattern was pushed at a Matrix whose impetus slot is empty or holds a fractured impetus. Those are refused outright rather than queued, so the network can look elsewhere.
* *Focusing disabled* means focusing is switched off for the Matrix in the config. Run the recipe on a Spirit Crucible, or turn **Spirit Focusing Enabled** back on.
* Recipes that carry an ingredient's own data over to the result only encode for a plain ingredient carrying no data of its own, the same rule the altar follows.
* Turn on Verbose Logging under Debug to have the Matrix log every pattern it refuses and why.
