---
navigation:
  title: Deploying
  icon: create:deployer
  parent: nep/nep-index.md
  position: 40
item_ids:
  - nep:andesite_crafting_pattern
---
# <Color id="aqua">Deploying</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Deploying</Color>

  <ItemImage id="create:deployer" scale="2"/>

  Automate the Deployer for `create:deploying` and `create:item_application` recipes. The Depot is the crafting machine: the base item is staged on it, and a Deployer two blocks above presses onto it.
</Column>

<Recipe id="nep:module_status/deploying"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="create:depot" y="0"/>
  <Block id="create:deployer" y="2" p:facing="down"/>

  <BlockAnnotation y="2" color="#ffaa00">
    **Deployer**, facing down, kept spinning
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#00ccff">
    **Depot**, and the Provider goes here
  </BlockAnnotation>
</GameScene>

* Deployer two blocks above the Depot, facing down, with rotational speed.
* The Provider goes on the <ItemLink id="create:depot"/>, not the Deployer.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

An <ItemLink id="nep:andesite_crafting_pattern"/>. Look the recipe up in JEI, open a <ItemLink id="ae2:pattern_encoding_terminal"/>, and click the **+** on the recipe: the blank pattern comes back as an Andesite Crafting Pattern, naming the exact `create:deploying` or `create:item_application` recipe to run. Inputs are the base item plus any deployed item the recipe <Color id="red">consumes</Color>.

A plain processing pattern with the same inputs and result still works; the Depot simply has to search the recipe list for a match instead of being told which one to run.

> <Color id="yellow">Reusable tools are not inputs.</Color> If the recipe keeps its deployed item, load that item into the Deployer yourself and leave it out of the pattern. Pushes are rejected until the Deployer holds it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Deployer must face down and be spinning.
* Depot must be empty when the craft starts.
* Only single, guaranteed-output recipes are automated.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Module Settings</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Under Modules → Create → Deploying.

| Setting | Default | Effect |
|---|---|---|
| Enabled | true | Turns Deploying on or off |

With it off, Deploying patterns stop encoding and Providers stop pushing to Depots. See [Overview](getting-started.md) for where the config lives and for the master switch.
