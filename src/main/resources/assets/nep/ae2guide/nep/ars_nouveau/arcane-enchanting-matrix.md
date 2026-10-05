---
navigation:
  title: Arcane Enchanting Matrix
  icon: nep:arcane_enchanting_matrix
  parent: nep/ars_nouveau/index.md
  position: 50
item_ids:
  - nep:arcane_enchanting_matrix
---
# <Color id="light_purple">Arcane Enchanting Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="light_purple">Arcane Enchanting Matrix</Color>

  <ItemImage id="nep:arcane_enchanting_matrix" scale="2"/>

  One block that replaces the Enchanting Apparatus with its ring of Arcane Pedestals and the Imbuement Chamber with its pedestals. It pays for every craft out of its own Source store, so no jar has to sit within reach of a pedestal.
</Column>

<Recipe id="nep:module_status/arcane_enchanting_matrix"/>

> <Color id="yellow">The block reads its own state.</Color> The Matrix is an open frame with a faceted core turning inside it, visible through every face. The core drifts while the Matrix is idle, spins up and brightens as a craft runs, and flares when the craft lands. A stalled Matrix dims and judders in place, and its frame goes dark.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="nep:arcane_enchanting_matrix" z="0"/>
  <Block id="ae2:pattern_provider" z="1"/>
</GameScene>

* Put a Pattern Provider against any Matrix face and drop in <ItemLink id="nep:apparatus_pattern"/> or <ItemLink id="nep:imbuement_pattern"/> patterns, or plain processing patterns of either. The same patterns that drive the real machines drive the Matrix.
* Enchantments and Armour Upgrades run here too, one pattern per enchantment level, exactly as they do on the real apparatus.
* The Matrix is an ME machine: it joins the network through its own grid connection, draws a small standing power cost, and returns finished items to the Provider on its own. It needs no **Import Card**.
* It takes up <nep:ConfigValue name="arcaneEnchantingMatrixChannels"/> channels, so anything above 8 needs a dense cable running to it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Source</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Matrix holds up to 100,000 Source, shown in the gauge at the right of its screen. A craft pays its recipe's full Source cost the moment it starts.

* **Source Relays** can be linked straight into the Matrix with a Dominion Wand, like any other Source machine. Nothing can draw Source back out of it.
* **Source Jars** within <nep:ConfigValue name="arcaneEnchantingMatrixSourceJarRange"/> blocks are drawn from on their own, measured the way the Enchanting Apparatus measures its reach.
* **Ars Énergistique**, when installed, lets the Matrix draw Source stored in its ME network once the nearby jars run dry.
* The Matrix only ever draws what its queued crafts still need. It never empties nearby jars into a store it has no use for, so a jar shared with other Ars machines stays useful to them.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Borrowed Catalysts</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

An imbuement recipe's pedestal items are never used up, so they never belong in a pattern. Encode the reagent alone; a pattern that carries the pedestal items is refused.

* When an imbuement job arrives, the Matrix borrows one set of its pedestal items from the ME network and shows them on the **Catalysts** shelf.
* That one set serves every craft of the recipe. It stays on the shelf for as long as any crafting job still has crafts of that recipe left to send, then goes back to the network in one piece.
* With **Auto-Request Recipes** on, a pedestal item the network does not have is crafted for you. Without the network, drop the pedestal items into the input slots by hand.
* The shelf holds two sets at once. A third imbuement recipe waits until one of them is handed back.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Glyph Upgrades</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The two slots between the input grid and the Source gauge take spell glyphs.

* <ItemLink id="ars_nouveau:glyph_accelerate"/> shortens every craft. An Enchanting Apparatus craft takes <nep:ConfigValue name="arcaneEnchantingMatrixApparatusCraftTicks"/> ticks and an imbuement <nep:ConfigValue name="arcaneEnchantingMatrixImbuementCraftTicks"/>; a full stack of <nep:ConfigValue name="arcaneEnchantingMatrixMaxAccelerate"/> brings both down to <nep:ConfigValue name="arcaneEnchantingMatrixMinimumCraftTicks"/>.
* <ItemLink id="ars_nouveau:glyph_dampen"/> lowers the Source every craft costs. A full stack of <nep:ConfigValue name="arcaneEnchantingMatrixMaxDampen"/> takes <nep:ConfigValue name="arcaneEnchantingMatrixSourceDiscount"/>% off.
* Fewer glyphs give a proportional share of either effect.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The readout names what is being made, how many jobs are queued, the Source stored and how long a craft takes with the glyphs installed.
* The three buttons clear the pending jobs, empty the buffers into your inventory, and open this page. Emptying also hands you the catalyst shelf while no craft is running.
* A Comparator beside the Matrix reads the output buffer, empty for nothing staged up to a full 15.
* Input slots only accept what the Matrix is actually short of, so a hopper can top it up without stuffing it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Crafting By Hand</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

An Enchanting Apparatus or imbuement recipe can also be started out of your own inventory. Open the Matrix, find the recipe in the recipe viewer, and press its transfer button: every ingredient leaves your inventory in one go and the craft queues up like any other.

* Holding shift queues as many crafts as your inventory can pay for, up to sixty-four.
* The finished item waits in the output buffer for you to take.
* Imbuement pedestal items still come from the network or the input slots, never from your inventory.
* Enchantments and Armour Upgrades are pattern only.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **No network power.** The Matrix needs a powered ME network before it starts anything.
* **Waiting on Source.** The store cannot pay for the next craft yet. It starts by itself once enough Source arrives.
* **Waiting on catalysts.** An imbuement recipe's pedestal items are not on the shelf and could not be borrowed.
* **Catalyst shelf full.** Two other imbuement recipes are holding their sets. The job starts once one of them is handed back.
* **Output buffer full.** A finished craft with nowhere to go holds the machine at full progress until the buffer is cleared.
* **Two recipes for one item.** Only one recipe per result item can be queued at a time.
