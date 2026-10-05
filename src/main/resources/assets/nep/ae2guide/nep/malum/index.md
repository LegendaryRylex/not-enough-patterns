---
navigation:
  title: Malum
  icon: malum:spirit_altar
  parent: nep/nep-index.md
  position: 90
---
# <Color id="light_purple">Malum</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="light_purple">Malum</Color>

  <ItemImage id="malum:spirit_altar" scale="2"/>

  Runs spirit infusion, spirit focusing and runeworking as ME auto-crafting steps, on the real Spirit Altar, Spirit Crucible and Runic Workbench with their own spirits, pedestals, augments and accelerators, or on a Matrix that stands in for all three.
</Column>

<Recipe id="nep:module_status/malum"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="malum:spirit_altar" scale="0.5"/> Spirit Infusion | Spirit Altar, with pedestals for recipes that need extra ingredients | Spirit Infusion Pattern |
| <ItemImage id="malum:spirit_crucible" scale="0.5"/> Spirit Focusing | Spirit Crucible, holding the impetus for the node you want | Spirit Focusing Pattern |
| <ItemImage id="malum:runic_workbench" scale="0.5"/> Runeworking | Runic Workbench, with the second ingredient supplied from the network instead of your hand | Runeworking Pattern |
| <ItemImage id="nep:focused_spirit_matrix" scale="0.5"/> Spirit Infusion | Focused Spirit Matrix, one block, nothing built around it | Spirit Infusion Pattern |
| <ItemImage id="nep:focused_spirit_matrix" scale="0.5"/> Spirit Focusing | Focused Spirit Matrix, with a Matrix Impetus in its impetus slot | Spirit Focusing Pattern |
| <ItemImage id="nep:focused_spirit_matrix" scale="0.5"/> Runeworking | Focused Spirit Matrix, drawing a rune's spirits from its own bank | Runeworking Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Secrets of the Void</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Malum keeps its void tier out of sight until you uncover the Secrets of the Void, and a recipe hidden that way is absent rather than locked, which is easy to read as a recipe that does not exist. Any of our items whose every recipe is hidden carries a line saying as much, and the line goes as soon as the revelation that hides it is uncovered.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:malum_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Malum patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:malum_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
