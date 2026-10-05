---
navigation:
  title: Apothic Enchanting
  icon: apothic_enchanting:raven_enchanting_table
  parent: nep/nep-index.md
  position: 40
---
# <Color id="aqua">Apothic Enchanting</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Apothic Enchanting</Color>

  <ItemImage id="apothic_enchanting:raven_enchanting_table" scale="2"/>

  Runs enchantment infusion as an ME auto-crafting step on the Table of the Raven, regardless of any shelves around it.
</Column>

<Recipe id="nep:module_status/apothic_enchanting"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="apothic_enchanting:raven_enchanting_table" scale="0.5"/> Enchantment Infusion | Table of the Raven, shelves optional | Processing Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:apothic_enchanting_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Apothic Enchanting patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:apothic_enchanting_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
