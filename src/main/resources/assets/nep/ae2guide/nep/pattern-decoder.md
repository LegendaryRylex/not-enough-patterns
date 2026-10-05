---
navigation:
  title: Pattern Decoder
  icon: nep:pattern_decoder
  parent: nep/nep-index.md
  position: 5
item_ids:
  - nep:pattern_decoder
---
# <Color id="aqua">Pattern Decoder</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Pattern Decoder</Color>

  <ItemImage id="nep:pattern_decoder" scale="2"/>

  A network block that a server can make NEP patterns depend on. While the server requires it, a Pattern Encoding Terminal only encodes a mod's NEP patterns when a Pattern Decoder on the same network carries that mod's <Color id="yellow">Encoding Module</Color>.
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Does My Server Need One?</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Only if the server says so. **Require Decoder** in the server config is off by default, and while it is off every NEP pattern encodes the way it always has and the Decoder does nothing at all.

With it on, the config lists every integration under **Integrations**, and each one can be switched off on its own. A mod switched off there never needs a module; a mod left on needs its module before its patterns encode.

While **Require Decoder** is off, the Decoder and its modules cannot be crafted and are hidden from the recipe viewer.

Open a Decoder to see where your network stands. Every module has its own slot, shown faded until the module is in it, and hovering a slot names its state:

* <Color id="green">Active Module</Color>: the slot glows green. The module is in and the Decoder is online, so that mod's patterns encode as NEP patterns across the whole network.
* <Color id="yellow">Inactive Module</Color>: the module is missing, or the Decoder has no power or channel. The light in the title bar shows which.
* <Color id="red">Disabled in Config</Color>: a red cross over the slot. The server does not gate that mod, so its patterns encode without a module.
* <Color id="red">Mod Not Installed</Color>: a black slot with a red cross.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setting One Up</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

1. Place the Decoder anywhere on the same ME network as your Pattern Encoding Terminal. It takes up <nep:ConfigValue name="patternDecoderChannels"/> channel and draws <nep:ConfigValue name="patternDecoderIdleMeDrain"/> AE per tick, and it only counts while it is powered and has its channel.
2. Put the Encoding Modules you need into their slots; each module only fits its own. Each mod's module is shown on that mod's page in this guide, crafted around one of that mod's own materials.
3. Encode patterns as usual. One Decoder covers the whole network, and several Decoders on one network add their modules together.

A wireless terminal counts as part of the network it is linked to, so it encodes whatever that network's Decoders unlock while it is in range.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Without the Module</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Encoding never fails because of a missing module. The terminal writes a plain AE2 <ItemLink id="ae2:processing_pattern"/> with the same ingredients and result instead, and tells you once per session, per mod, which module would have made it a NEP pattern.

The Decoder only decides what the terminal writes. Patterns already encoded keep working whether or not a Decoder is around, so taking one away never stops a running setup.

<ItemImage id="minecraft:air" scale="0.25"/>

<Recipe id="nep:pattern_decoder"/>
