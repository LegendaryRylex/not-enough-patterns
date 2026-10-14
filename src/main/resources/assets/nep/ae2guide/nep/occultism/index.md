---
navigation:
  title: Occultism
  icon: occultism:golden_sacrificial_bowl
  parent: nep/nep-index.md
  position: 92
---
# <Color id="aqua">Occultism</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Occultism</Color>

  <ItemImage id="occultism:golden_sacrificial_bowl" scale="2"/>

  Puts Occultism's spirit work into ME auto-crafting: spirit fire, crushing and crystallizing inside a single Matrix, real working spirits fed straight from a Pattern Provider, and crafting rituals run on your own pentacle.
</Column>

<Recipe id="nep:module_status/occultism"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Supported Machines</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Process | Setup | Pattern |
|---|---|---|
| <ItemImage id="nep:bound_spirit_matrix" scale="0.5"/> Spirit fire, crushing and crystallizing | A single Matrix, with a Spirit-Infused Book for crushing or crystallizing | Processing Pattern |
| <ItemImage id="nep:spirit_provider" scale="0.5"/> Crushing, crystallizing, smelting and trading | A Spirit Provider inside a working spirit's work area | Processing Pattern |
| <ItemImage id="nep:ritual_controller" scale="0.5"/> Crafting rituals | A Ritual Controller beside a pentacle's golden bowl, with a Spirit-Infused Book | Ritual Pattern |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Encoding Module</Color>

  <ItemImage id="nep:occultism_encoding_module" scale="1.5"/>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a server that turns on **Require Decoder**, Ritual Patterns only encode on a network whose [Pattern Decoder](/nep/pattern-decoder.md) carries this module. Without it they encode as plain Processing Patterns.

<Recipe id="nep:occultism_encoding_module"/>

<ItemImage id="minecraft:air" scale="0.5"/>

<SubPages icons={true} />
