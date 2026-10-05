---
navigation:
  title: Deploying
  icon: create:deployer
  parent: nep/create/index.md
  position: 61
item_ids:
  - nep:andesite_crafting_pattern
---
# <Color id="gold">Deploying</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Deploying</Color>

  <ItemImage id="create:deployer" scale="2"/>

  Automate the Deployer for `create:deploying`, `create:item_application` and `create:sandpaper_polishing` recipes. The Depot is the crafting machine: the base item is staged on it, and a Deployer two blocks above presses onto it.
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
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

An <ItemLink id="nep:andesite_crafting_pattern"/>. Look the recipe up in JEI, open a <ItemLink id="ae2:pattern_encoding_terminal"/>, and click the **+** on the recipe: the blank pattern comes back as an Andesite Crafting Pattern, naming the exact `create:deploying` or `create:item_application` recipe to run. Inputs are the base item plus the deployed item.

A plain processing pattern with the same inputs and result still works; the Depot simply has to search the recipe list for a match instead of being told which one to run.

> <Color id="yellow">Reusable tools are returned.</Color> When the recipe keeps its deployed item, that slot is marked <Color id="yellow">retained</Color> in the encoding terminal. The network lends the tool for the craft and gets it back afterwards, so it is never spent, and a Provider hands it straight to the Deployer instead of you loading one by hand.

If the Deployer is already holding a matching tool, that one is used and the lent copy goes straight back to the Provider untouched. A pattern with the tool left out still works too, but then the Deployer has to be loaded by hand and pushes are rejected until it is.

> <Color id="yellow">Tools a Deployer wears down are returned used.</Color> Sand paper, and any tool a recipe deploys without keeping, loses a point of durability per craft rather than being spent whole. Those patterns ask for the tool back one point worse, so the network can lend a part-used one and gets that same one back. A tool on its last point breaks in the Deployer and nothing comes back, which is exactly what the pattern asked for.

A worn tool is always lent fresh for each craft, so the Deployer has to be empty when the pattern arrives. Pushes are turned away while it is still holding something, because the network is owed back the tool the pattern handed over, not whatever was left in the Deployer.

An [Assembly Matrix](/nep/create/sequenced-assembly-matrix.md) takes the same pattern and deploys inside itself, with no Depot and no Deployer. It holds nothing by hand, so there the tool must be in the pattern, and it turns down the rare recipe whose tool a Deployer wears down instead of consuming.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Sanding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<Row>
  <ItemImage id="create:rose_quartz" scale="1.5"/>
  <ItemImage id="create:sand_paper" scale="1.5"/>
  <ItemImage id="create:polished_rose_quartz" scale="1.5"/>
</Row>

A Deployer holding <ItemLink id="create:sand_paper"/> polishes whatever is on the Depot, but Create files those recipes under their own `create:sandpaper_polishing` type rather than Deploying, so a pattern had nothing to name and the **+** in JEI handed back a plain processing pattern that no Depot would take. NEP now reads every polishing recipe as a Deploying one, and the **+** on Create's **Sandpaper Polishing** entry gives an <ItemLink id="nep:andesite_crafting_pattern"/> like any other.

* The pattern holds the base item and one sheet, with the sheet <Color id="yellow">retained</Color>. Either <ItemLink id="create:sand_paper"/> or <ItemLink id="create:red_sand_paper"/> satisfies it.
* A Deployer sands its sheet down a point per polish, unlike the axes it strips with. The pattern therefore names the sheet as a tool that comes back one point worse, and the Deployer is emptied again after every polish.
* Any damage level satisfies the pattern, so a part-used sheet in storage is stock like any other and the network only makes a fresh one once every sheet it holds has worn through. A sheet still costs one polish per point, eight in all.
* The polish that spends a sheet's last point breaks it in the Deployer, and the pattern asked for nothing back, so the craft settles with the base item alone.
* An [Assembly Matrix](/nep/create/sequenced-assembly-matrix.md) turns polishing down for the same reason it turns down any recipe that wears its tool: it hands retained inputs back exactly as they arrived.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Stripping Logs</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<Row>
  <ItemImage id="minecraft:oak_log" scale="1.5"/>
  <ItemImage id="minecraft:iron_axe" scale="1.5"/>
  <ItemImage id="minecraft:stripped_oak_log" scale="1.5"/>
</Row>

JEI lists stripping under **Item Application**, but Create shows that only as a reminder of the vanilla right click: there is no recipe behind it, so a Deployer normally ignores a log sitting on a Depot. The **Log Stripping** setting adds the missing recipe for every log an axe can strip, and lists it under **Deploying** alongside the recipes a Deployer can already run.

* The axe is neither consumed nor damaged, exactly like Create's own axe recipes for de-oxidising copper, so the network lends the same one to every craft.
* The pattern holds the log and the axe, with the axe retained. Encode it from either JEI entry with **+**, or by hand as a processing pattern taking the log and giving the stripped log, with the axe left in the Deployer.
* It works for a Depot filled by hand too, not only one driven by a Provider.
* A log that a loaded recipe already applies an axe to is left alone, so a pack can replace what stripping it does simply by shipping its own `create:deploying` or `create:item_application` recipe.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Deployer must face down and be spinning.
* Depot must be empty when the craft starts.
* Only single, guaranteed-output recipes are automated.