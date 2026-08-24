---
navigation:
  title: Import Card
  icon: nep:import_card
  parent: nep/nep-index.md
  position: 2
item_ids:
  - nep:import_card
---
# <Color id="aqua">Import Card</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Import Card</Color>

  <ItemImage id="nep:import_card" scale="2"/>

  Slot it into a Pattern Provider and that Provider collects its own results. This is what closes the loop for machines that leave their output in their buffer instead of pushing it back to the system.
</Column>

<ItemImage id="minecraft:air" scale="0.5"/>

<RecipeFor id="nep:import_card"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How It Works</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Provider remembers what dispatched recipe it still owes and which side it sent that pattern to.
* It only ever collects those exact results, from that exact side, up to the amount it is still waiting for.
* Anything else sharing the inventory is left alone, including the ingredients the Provider just staged there for a job in progress.

Because the Provider that pushed is the Provider that collects, results are always credited to the job that asked for them. Two Providers working the same block never take each other's items.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Cancelled Jobs</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Cancel a job partway through and whatever the Provider had already sent out will still be collected by the card for a short period afterwards, so the results in flight at the moment you cancelled comes back to the network instead of being stranded in the machine. Every side collects its own, so a Provider feeding several stations gets one back from each.

The grace restarts each time the card collects something, and only starts once the job has ended. While a job is live the card waits for its results however long they take.

The grace period is <nep:ConfigValue name="importCardGrace"/> ticks under `importCardGrace`.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Works In</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Beyond AE2's own Pattern Provider, in block and cable form, the card slots into the pattern providers other mods add: Advanced AE's Advanced and Small Advanced Pattern Providers, ExtendedAE's Extended Pattern Provider, and MEGA Cells' MEGA Pattern Provider.
