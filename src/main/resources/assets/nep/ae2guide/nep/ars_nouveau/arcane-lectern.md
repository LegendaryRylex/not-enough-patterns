---
navigation:
  title: Arcane Lectern
  icon: nep:arcane_lectern
  parent: nep/ars_nouveau/index.md
  position: 30
item_ids:
  - nep:arcane_lectern
---
# <Color id="gold">Arcane Lectern</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Arcane Lectern</Color>

  <ItemImage id="nep:arcane_lectern" scale="2"/>

  A Storage Lectern presents its linked chests to Ars Nouveau as one inventory. The Arcane Lectern presents an ME network the same way, so anything that takes items from a neighbouring inventory can take them from the network instead.
</Column>

<Recipe id="nep:module_status/arcane_lectern"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="nep:arcane_lectern" y="0"/>
  <BlockAnnotation y="0" color="#88cc00">
    **Arcane Lectern**, near a Scribes Table
  </BlockAnnotation>
</GameScene>

* Wire the Lectern into an ME network. It is a network machine, so it needs channels and power like any other.
* Stand it within <nep:ConfigValue name="arcaneLecternScribesRange"/> blocks of a Scribes Table. **Scribes Table Range** in the config moves that distance.
* Set a glyph recipe on the table as usual. The Lectern sends the reagents the table is still missing, all at once, and sends nothing between crafts.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What Else It Serves</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Lectern is not a Scribes Table accessory. It exposes an ordinary inventory, which is what most of Ars Nouveau's item logic reads, so the same block feeds several things at once:

| Reader | What it takes |
|---|---|
| Scribes Table | Glyph reagents |
| Wixie Cauldron | Ingredients its Wixie fetches |
| Starbuncle | Anything on its route |
| Amethyst Golem | What it deposits and picks up |
| Item Detector | A count to compare against |

Addon machines built on any of those get it too. An Enchanting Wixie Cauldron is a Wixie Cauldron underneath, so a Lectern in reach supplies its apparatus ingredients without a pattern being involved at all.

> <Color id="yellow">This cuts both ways.</Color> A Starbuncle within reach will happily haul network contents off to wherever it was told to put things, so place the Lectern where you mean it to be read.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Cost</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Lectern never pushes. It waits to be read, and everything reading it walks the whole offered list once per pass, so a very large network is cheaper to serve with a lower cap. `Maximum Item Types` in the config sets that cap, and items beyond it are simply not offered.

Nothing is inserted through the Lectern either. It is a read-only window onto the network, so anything that needs to go the other way needs its own route in.
