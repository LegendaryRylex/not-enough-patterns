---
navigation:
  title: Ars Nouveau
  icon: ars_nouveau:enchanting_apparatus
  parent: nep/nep-index.md
  position: 85
---
# <Color id="aqua">Ars Nouveau</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Ars Nouveau</Color>

  <ItemImage id="ars_nouveau:enchanting_apparatus" scale="2"/>

  Drives the pedestal machines as ME auto-crafting steps, folds both into a single Matrix, opens the network to Ars Nouveau's own item pathways, and keeps a Ritual Brazier running without a player standing over it.
</Column>

<Recipe id="nep:module_status/ars_nouveau"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="ars_nouveau:enchanting_apparatus" scale="0.5"/> Enchanting | Enchanting Apparatus with its Arcane Pedestals | Apparatus Pattern |
| <ItemImage id="ars_nouveau:imbuement_chamber" scale="0.5"/> Imbuement | Imbuement Chamber with any touching Arcane Pedestals | Imbuement Pattern |
| <ItemImage id="nep:arcane_lectern" scale="0.5"/> Glyph scribing | Arcane Lectern near a Scribes Table | None, the table pulls |
| <ItemImage id="nep:ritual_conductor" scale="0.5"/> Rituals | Ritual Conductor by a Ritual Brazier | None, rituals have no output |
| <ItemImage id="nep:arcane_enchanting_matrix" scale="0.5"/> Enchanting and imbuement | Arcane Enchanting Matrix, no pedestals | Apparatus or Imbuement Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Source Is Still Yours</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Nothing here supplies source. The real machines draw it the way Ars Nouveau intends, out of Source Jars within their own reach, so a working setup still needs source production next to it. The [Arcane Enchanting Matrix](/nep/ars_nouveau/arcane-enchanting-matrix.md) keeps a store of its own, filled by Source Relays, nearby jars, or the ME network through Ars Énergistique.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:ars_nouveau_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Ars Nouveau patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:ars_nouveau_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
