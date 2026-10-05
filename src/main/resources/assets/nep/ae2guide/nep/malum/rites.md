---
navigation:
  title: Rites
  icon: nep:pure_spirit
  parent: nep/malum/index.md
  position: 26
---
# <Color id="light_purple">Rites</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="light_purple">Rites</Color>

  <ItemImage id="nep:pure_spirit" scale="2"/>

  A Pure Spirit carves a totem pole like any of Malum's eight. Two rites read that carving, and both take a Malum rite's totem and put a Pure Spirit on top of it.
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Carve the pole with a <ItemLink id="nep:pure_spirit"/> the way you would with any spirit shard, then light the totem base as usual. Nothing else about totemancy changes, and the pole strips back to a plain log.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Rite Of Preservation</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Totem | Spirits |
|---|---|
| Runewood | Eldritch, Arcane, Sacred, Sacred, Pure |

Every <ItemLink id="nep:focused_spirit_matrix"/> and every <ItemLink id="malum:spirit_crucible"/> within eight blocks spends no impetus durability while the rite burns. A machine under the rite never fractures its impetus, which is the difference between one that needs watching and one that does not.

The rite holds the wear off rather than mending it, so an impetus already part worn stays where it is. A Matrix Catalyzer does not mend it while the rite burns either. On a crucible the Mending Diffuser and the Shielding Apparatus only act on a craft that spends durability, so both sit idle while the rite burns.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Rite Of Reaping</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

| Totem | Spirits |
|---|---|
| Soulwood | Eldritch, Arcane, Wicked, Wicked, Pure |

Malum's Rite of Rending with the harvest attached. Every second the rite sweeps eight blocks for monsters at or below a quarter of their health and sends a spark at each one it finds, and the spark strikes a fatal blow on arrival. Every loose spirit inside that range is drawn into the nearest ME network with room for it instead of drifting off.

The sparks can be turned off in the config for a pack that spawns monsters faster than sparks can chase them. The blow then lands the moment the sweep finds the monster, with nothing in the air to wait on.

The network is found from any grid-connected block within the same eight blocks, so a cable end, a Pattern Provider or a Focused Spirit Matrix near the totem is enough. With none in range the rite still rends, and the spirits stay in the world.

<ItemImage id="minecraft:air" scale="0.5"/>
