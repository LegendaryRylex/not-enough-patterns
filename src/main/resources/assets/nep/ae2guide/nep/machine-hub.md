---
navigation:
  title: Machine Hub
  icon: nep:machine_hub
  parent: nep/nep-index.md
  position: 4
item_ids:
  - nep:machine_hub
  - nep:hub_linker
---
# <Color id="aqua">Machine Hub</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Machine Hub</Color>

  <ItemImage id="nep:machine_hub" scale="2"/>

  A block that stands in front of a machine whose inventories are scattered across several blocks. Link its hatches, ports and buffers to the Hub, put a Pattern Provider against the Hub, and the whole machine looks like one inventory to your network.
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Problem It Solves</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A Pattern Provider pushes a whole pattern into <Color id="yellow">one</Color> inventory. It never splits a recipe across two.

That is fine for a machine with a single inventory, and useless for the sort of multiblock a pack builds out of separate parts: an item input hatch here, a fluid input hatch there, an output hatch somewhere else. Each part is its own block with its own inventory, and the controller in the middle usually exposes nothing at all. A recipe that wants three items and a bucket of something has no single place to land.

The Hub is that single place. It takes the whole pattern and hands each ingredient on to whichever linked inventory will have it.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setting One Up</Color>

  <Row>
    <ItemImage id="nep:machine_hub" scale="1.5"/>
    <ItemImage id="nep:hub_linker" scale="1.5"/>
  </Row>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

1. Place the Hub <Color id="yellow">touching the machine</Color>, and put a Pattern Provider against the Hub.
2. Open the Hub and press <Color id="green">Scan</Color>. It walks outward through the machine, links everything it finds that holds items or fluids, and guesses whether each one takes ingredients or gives results.
3. Check the guesses. Every row has a <Color id="yellow">→</Color> or <Color id="yellow">←</Color> you can click to swap, and an <Color id="red">✗</Color> that un-links it. Twelve rows show at a time; scroll the wheel or drag the bar for the rest.

The scan spreads from block to touching block, so it only ever finds parts of the machine the Hub is up against. It travels freely through <Color id="yellow">machine parts</Color>, meaning any block that carries a block entity of its own: every hatch, port, buffer and controller, including the controllers that expose no inventory at all. A vanilla block entity has to hold something before it counts, so a lectern or a sign is not a bridge to somewhere else.

Everything else is casing. Casing is only stepped through for a short run at a time, set by `Casing Depth` and reset at every machine part, and only when it belongs to a mod that already owns a machine part nearby. That is what lets the scan cross a wall to the hatch on the other side without ever wandering off into terrain or a mod's decorative blocks. It turns back at anything on an ME network, so the Provider feeding the Hub is never picked up. Reach is capped by `Link Range` and total work by `Scan Budget`.

For anything the scan gets wrong, or a machine sat among other inventories you do not want touched, use the <ItemLink id="nep:hub_linker"/> instead. Sneak and use it in the air to pick Input or Output, use it on each block you want, then use it on the Hub to apply the whole plan at once. Using it on an already-planned block takes that block back out.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Input and Output</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<Color id="yellow">Input</Color> inventories are where ingredients go. The Hub fills them in the order they are listed, so put the fussy ones first if a machine is picky about which hatch gets what.

<Color id="yellow">Output</Color> inventories are never pushed into. The Hub empties them into your network for you, a few times a second, so results go straight back into storage without an Import Card or a Storage Bus. It reaches the network through whatever it is standing against, which is normally the Provider feeding it.

Nothing is ever taken out of a machine before the network has agreed to take it, so a network with no room leaves the results sitting in the machine. When that happens, or when there is no network against the Hub at all, the Hub's screen says so in <Color id="red">red</Color> across the top.

Everything linked, whichever role it carries, is readable through the Hub. That is what Blocking Mode reads when it decides the machine is still busy, and it is also what a Storage Bus on the Hub would see.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What It Does Not Do</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Hub moves items and fluids and nothing else. It does not know the machine's recipes, does not check that the structure is built, does not supply power, and does not wait for a craft to finish. The machine runs itself exactly as it would if you had piped everything in by hand.

<Color id="red">It also cannot invent an inventory that is not there.</Color> If a machine's parts expose nothing to the outside world, there is nothing for the Hub to link, and no Provider of any kind was ever going to reach it.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Configs for Pack Devs</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

`Machine Hub → Enabled` stops every placed Hub offering storage, so Providers ignore them, while leaving their links intact for when it is switched back on.

`Link Range` caps how far a linked inventory may sit from the Hub, and is also how far a scan may reach. `Maximum Links` caps how many links a Hub holds and how many a scan may propose; the screen shows twelve at a time and scrolls past that. `Scan Budget` caps how many blocks one scan may walk through, which is the whole cost of pressing the button. `Casing Depth` sets how far a scan may follow plain blocks between one machine part and the next, and setting it to zero makes the scan follow machine parts alone.
