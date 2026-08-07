---
navigation:
  title: Fusion Crafting
  icon: draconicevolution:crafting_core
  parent: nep/draconicevolution/index.md
  position: 10
item_ids:
  - nep:fusion_crafting_pattern
---
# <Color id="gold">Fusion Crafting</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Fusion Crafting</Color>

  <ItemImage id="draconicevolution:crafting_core" scale="2"/>

  Automate Draconic Evolution's Fusion Crafting. The <ItemLink id="draconicevolution:crafting_core"/> is the crafting machine: the network stages the catalyst in the core, loads every Crafting Injector around it, and starts the craft.
</Column>

<Recipe id="nep:module_status/fusion_crafting"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="3">
  <Block id="draconicevolution:crafting_core" x="0" y="0" z="0"/>
  <Block id="draconicevolution:wyvern_crafting_injector" x="-2" y="0" z="0" p:facing="east"/>
  <Block id="draconicevolution:wyvern_crafting_injector" x="2" y="0" z="0" p:facing="west"/>
  <Block id="draconicevolution:wyvern_crafting_injector" x="0" y="0" z="-2" p:facing="south"/>
  <Block id="ae2:pattern_provider" x="0" y="1" z="0"/>

  <BlockAnnotation x="0" y="1" z="0" color="#00ccff">
    **Pattern Provider**, touching the Core
  </BlockAnnotation>
  <BlockAnnotation x="0" y="0" z="0" color="#ff55ff">
    **Crafting Core**, holding the catalyst
  </BlockAnnotation>
  <BlockAnnotation x="-2" y="0" z="0" color="#ffaa00">
    **Crafting Injectors**, facing the Core and powered
  </BlockAnnotation>
</GameScene>

* Build the Fusion Crafting multiblock exactly as Draconic Evolution wants it: Injectors in straight cardinal lines, facing the Core, at least Draconic Evolution's minimum distance away and within its injector range.
* The Provider goes on the <ItemLink id="draconicevolution:crafting_core"/>, not on an Injector.
* Injectors still need power. The network loads them with items; it does not charge them.
* Add an <ItemLink id="nep:import_card"/> to the Provider so the finished item comes back out of the Core into the network.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Injector Tier</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Every Injector that holds an ingredient has to be at least the recipe's tier, so a Chaotic recipe needs Chaotic Injectors. The network only fills Injectors that meet the tier, and it needs enough of them to cover the recipe's ingredient list.

Lower-tier Injectors may sit in the multiblock as long as they are empty. If one is holding an item, the push is rejected, because Draconic Evolution would refuse the craft.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A <ItemLink id="nep:fusion_crafting_pattern"/>. Look the recipe up in JEI, open a <ItemLink id="ae2:pattern_encoding_terminal"/>, and click the **+** on the recipe: the blank pattern comes back as a Fusion Crafting Pattern, naming the exact `draconicevolution:fusion_crafting` recipe to run. Inputs are the catalyst plus every ingredient the recipe <Color id="red">consumes</Color>, and any it <Color id="gold">keeps</Color> ride along as retained inputs.

A plain processing pattern with the same inputs and result still works; the Core simply has to search the recipe list for a match instead of being told which one to run.

> <Color id="yellow">Gear upgrades automate, and keep what the old item held.</Color> A fusion upgrade carries the catalyst's energy, modules and enchantments onto the new item, exactly as a hand craft does. The network sources a fresh, empty tool for the job, so an upgrade requested through the ME system never reaches for the loaded one you are carrying. If you want your own tool upgraded, put it in the Core yourself.

> <Color id="yellow">Kept ingredients are borrowed, not spent.</Color> If a recipe keeps an ingredient instead of consuming it, the pattern carries it like any other input: the Core loads it into an Injector for the craft and hands it straight back to the Pattern Provider once the craft ends, whether it finished or was cancelled. One is enough for a job of any size. A pattern encoded without it still works from a copy you load into an Injector yourself, which is then checked and left alone.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Core must be idle, with an empty catalyst slot and nothing blocking its output slot.
* Every Injector holding an item must be at the recipe's tier or better, and there must be enough of them.
* Injectors need energy to charge. A craft that has started sits at *Charging* until they are fed.
* Turn on Verbose Logging under Debug to have the Core log why each push was refused.