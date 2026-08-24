---
navigation:
  title: Injector Fusion Matrix
  icon: nep:fusion_matrix
  parent: nep/draconicevolution/index.md
  position: 20
item_ids:
  - nep:fusion_matrix
---
# <Color id="gold">Injector Fusion Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Injector Fusion Matrix</Color>

  <ItemImage id="nep:fusion_matrix" scale="2"/>

  One block that runs a whole fusion craft. No Crafting Core, no Injectors, no alignment: the Matrix stages the ingredients itself, charges on raw energy, and hands the finished item back to the network.
</Column>

<Recipe id="nep:module_status/fusion_matrix"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="nep:fusion_matrix" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#ffaa00">
    **Injector Fusion Matrix**, wired to energy and to the ME network
  </BlockAnnotation>
</GameScene>

* Put a Pattern Provider against any face and drop <ItemLink id="nep:fusion_crafting_pattern"/> patterns in it. The Matrix buffers the ingredients as they arrive.
* Feed it **FE** on any face, or Draconic Evolution's own OP, which counts the same. This is the energy the craft is made of, and it replaces the Injectors entirely.
* The Matrix is also an ME machine: it joins the network through its own grid connection, draws a small standing power cost, and returns finished items to the Provider on its own. It needs **no Import Card**.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How It Crafts</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A craft goes through two phases, shown on the bar in the Matrix's screen.

| Phase | What is happening |
|---|---|
| <Color id="red">Charging</Color> | The Matrix is pulling energy in, up to its charge rate per tick, until it has banked the recipe's full energy cost |
| <Color id="gold">Fusing</Color> | The energy is spent and the item is assembled over a fixed number of ticks |

Charging is where nearly all the time goes, and it is entirely down to how much power you can push at the Matrix. A recipe costs exactly what Draconic Evolution says it costs, so a Chaotic recipe still wants the same hundred million FE the real multiblock would have burned.

> <Color id="yellow">Gear upgrades automate, and keep what the old item held.</Color> The Matrix assembles gear the same way a Crafting Core does, carrying the catalyst's energy, modules and enchantments onto the result. The network sources a fresh, empty tool for the job, so an upgrade requested through the ME system never reaches for the loaded one you are carrying.

> <Color id="yellow">Kept ingredients come and go.</Color> If a recipe keeps an ingredient instead of consuming it, the pattern carries it like any other input: the Matrix borrows one for the craft and returns it untouched when the craft ends. One is enough for a job of any size, because the network gets it back before the next execution starts. A pattern encoded without it works too, from a copy you load into the input buffer by hand.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Upgrade Cores</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The slot on the right of the screen takes a stack of up to sixteen Draconic Evolution cores. One slot means one tier at a time, and a full slot is what reaches a tier's ceiling, so a half-filled slot lands part way there.

Each tier's buffer is sized for the tier it belongs to: the priciest fusion recipe at that tier, or the next tier's core, whichever costs more. Slot the cores for the tier you are actually crafting and the buffer covers it.

| Core | Buffer at a full slot | Also buys |
|---|---|---|
| <ItemLink id="draconicevolution:wyvern_core"/> | 50M FE, enough for an Awakened Draconium Block | Nothing. Wyvern Cores are buffer and only buffer |
| <ItemLink id="draconicevolution:awakened_core"/> | 256M FE, enough for a Draconic Staff of Power | Up to a 38% shorter fusing phase |
| <ItemLink id="draconicevolution:chaotic_core"/> | 1.024B FE, enough for a Chaotic Staff of Power | Up to 50% off the recipe's energy cost, and a fusing phase that falls all the way to 4 ticks |

Wyvern Cores fill the buffer in a straight line, while Draconic and Chaotic Cores multiply, so their gains arrive late in the slot. A Draconium Core does nothing, so the slot will not take one. Hover the slot for the exact numbers your config is running.

> <Color id="yellow">Cores tune, they do not accelerate charging.</Color> A deeper buffer lets the Matrix bank energy while it is idle and ride out an uneven supply, but the charge rate is still the ceiling on how fast energy goes in. Chaotic Cores are the only ones that shrink the bill itself.

> <Color id="yellow">Feed a big buffer with Draconic Evolution's own energy.</Color> Forge Energy counts in 32 bits, so an FE cable reads any buffer past 2,147,483,647 as permanently full and stops pushing. The defaults stay under that line, but if you raise a tier's capacity past it, use OP transfer rather than FE.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The top readout names what is being fused, the phase, and how much of the energy cost is banked.
* The input buffer only accepts items a pending craft still needs, so loose items cannot clog it.
* The upgrade slot on the right holds the cores. The gauge beside it is the energy buffer, and it grows as cores go in.
* **Clear Pending Recipes** drops every craft the Matrix still owes, cancels ingredients it has on request, and cancels the crafting jobs on the network that were waiting on it.
* **Empty Buffers** returns everything staged to your inventory.
* **Guide** opens this page, and is always the rightmost button.
* A Comparator beside the block reads the output buffer, empty for nothing staged up to a full 15.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Matrix must be on a powered ME network, not just connected to one.
* The Matrix takes up <nep:ConfigValue name="fusionMatrixChannels"/> channels, and anything above 8 needs an ME Controller and a dense cable running to it. A network that cannot spare them leaves it offline.
* Check the recipe's tier is at or below the configured maximum. Above it, patterns are refused outright.
* A red *Missing ingredients* line means auto-request could not source something. Hover it for the list.
* Turn on Verbose Logging under Debug to have the Matrix log every pattern it refuses and why.