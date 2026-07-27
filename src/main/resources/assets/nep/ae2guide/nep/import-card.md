---
navigation:
  title: Import Card
  icon: nep:import_card
  parent: nep/nep-index.md
  position: 20
item_ids:
  - nep:import_card
---
# <Color id="aqua">Import Card</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Import Card</Color>

  <ItemImage id="nep:import_card" scale="2"/>

  Slot it into a Pattern Provider and that Provider collects its own results. This is what closes the loop for machines that leave their output sitting in the world instead of pushing it back.
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How It Works</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Provider remembers what each pattern it dispatched still owes it, and which side it sent that pattern to.
* It only ever collects those exact results, from that exact side, up to the amount it is still waiting for.
* Anything else sharing the inventory is left alone, including the ingredients the Provider just staged there for a job in progress.

Because the Provider that pushed is the Provider that collects, results are always credited to the job that asked for them. Two Providers working the same <ItemLink id="create:depot"/> never take each other's items.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Where It Reaches</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The card collects from the block the Provider is pushing to, so the result has to land there.

| Machine | Works |
|---|---|
| <ItemImage id="create:deployer" scale="0.5"/> Deploying | The result stays on the Depot |
| <ItemImage id="create:spout" scale="0.5"/> Filling | The result stays on the Depot |
| <ItemImage id="create:mechanical_crafter" scale="0.5"/> Mechanical Crafting | Only if the output is routed back into the Provider |

> <Color id="yellow">The Sequenced Assembly Controller and Matrix do not need this card.</Color> Both sit on the ME network themselves and return their results directly.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<RecipeFor id="nep:import_card"/>
