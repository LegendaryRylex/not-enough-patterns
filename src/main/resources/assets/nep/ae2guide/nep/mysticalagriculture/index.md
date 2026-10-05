---
navigation:
  title: Mystical Agriculture
  icon: mysticalagriculture:infusion_altar
  parent: nep/nep-index.md
  position: 80
---
# <Color id="aqua">Mystical Agriculture</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Mystical Agriculture</Color>

  <ItemImage id="mysticalagriculture:infusion_altar" scale="2"/>

  Runs infusion and awakening as ME auto-crafting steps, on either real altar with its pedestals or on a Matrix whose essence tanks stand in for the vessels.
</Column>

<Recipe id="nep:module_status/mysticalagriculture"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="mysticalagriculture:infusion_altar" scale="0.5"/> Infusion | Infusion Altar with eight Pedestals | Infusion Pattern |
| <ItemImage id="mysticalagriculture:awakening_altar" scale="0.5"/> Awakening | Awakening Altar with four Pedestals and four Vessels | Awakening Pattern |
| <ItemImage id="nep:infused_awakening_matrix" scale="0.5"/> Infusion and Awakening | A single Matrix, no altar | Infusion or Awakening Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:mystical_agriculture_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Mystical Agriculture patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:mystical_agriculture_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
