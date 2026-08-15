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
3. Check the guesses. Every row has a <Color id="yellow">→</Color> or <Color id="yellow">←</Color> you can click to cycle its role, a <Color id="yellow">⚙</Color> that opens that link's settings, and an <Color id="red">✗</Color> that un-links it. Twelve rows show at a time; scroll the wheel or drag the bar for the rest.

Scanning again keeps whatever you configured for a machine it finds a second time, so correcting a guess is never undone by a later scan.

The <Color id="aqua">?</Color> at the top right brings you back to this page. Every machine in the mod carries it in that same corner.

The scan spreads from block to touching block, so it only ever finds parts of the machine the Hub is up against. It travels freely through <Color id="yellow">machine parts</Color>, meaning any modded block that carries a block entity of its own, but only once that block's mod has shown the scan a part of the machine: every trail of trust starts at the Hub's own face. A machine standing two blocks over, from a mod the Hub is not touching, is left alone. A vanilla block entity only counts when it holds something and stands directly against the Hub, and common player storage, chests, barrels and shulker boxes, is never picked up by a scan at all; link those with the <ItemLink id="nep:hub_linker"/> if you really mean it.

Everything else is casing. Casing is only stepped through for a short run at a time, set by `Casing Depth` and reset at every machine part, and only when it belongs to a mod the scan already trusts or when the run starts against the Hub itself. That is what lets the scan cross a wall to the hatch on the other side without ever wandering off into terrain or a mod's decorative blocks. It turns back at anything on an ME network, so the Provider feeding the Hub is never picked up. Reach is capped by `Link Range` and total work by `Scan Budget`.

For anything the scan gets wrong, or a machine sat among other inventories you do not want touched, use the <ItemLink id="nep:hub_linker"/> instead. Sneak and use it in the air to pick a role, use it on each block you want, then use it on the Hub to apply the whole plan at once. Using it on an already-planned block takes that block back out.

Sneak and use a <Color id="yellow">wrench</Color> on a Hub to take it back once it is set up. The Hub goes straight into your inventory carrying every link and everything configured for them, and placing it again restores each link that is still in range of where you put it. Breaking the Hub instead gives you a plain one, with nothing remembered.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Input and Output</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<Color id="yellow">Input</Color> inventories are where ingredients go. Nothing is ever taken back out of one, so a Provider cannot undo its own delivery halfway through a craft.

<Color id="yellow">Output</Color> inventories are never pushed into. The Hub empties them into your network for you, a few times a second, so results go straight back into storage without an Import Card or a Storage Bus. It reaches the network through whatever it is standing against, which is normally the Provider feeding it.

<Color id="yellow">Input and output</Color> is for the machine that keeps its ingredients and its results in the same inventory, a furnace-shaped block rather than a hatch. It takes ingredients and gives results back from the one place, which means the Hub would happily hand your ingredients straight back to the network the moment they land: give such a link a <Color id="green">return filter</Color> naming only what the machine produces.

Nothing is ever taken out of a machine before the network has agreed to take it, so a network with no room leaves the results sitting in the machine. When that happens, or when there is no network against the Hub at all, the Hub's screen says so in <Color id="red">red</Color> across the top.

Everything linked, whichever role it carries, is readable through the Hub. That is what Blocking Mode reads when it decides the machine is still busy, and it is also what a Storage Bus on the Hub would see.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Configuring a Link</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The <Color id="yellow">⚙</Color> on any row opens that link on its own. The gear turns <Color id="gold">gold</Color> once a link carries anything other than its defaults, so a screen full of plain links shows at a glance which ones you have touched.

<Color id="yellow">Role</Color> is the same setting as the arrow on the row, spelled out.

<Color id="yellow">Face</Color> picks which side of the machine the Hub reaches through. <Color id="green">Any</Color>, the default, takes the block's un-sided inventory, which is the most permissive thing it offers and ignores whatever slot rules a machine only applies per face. Naming a face instead gets you exactly the inventory a pipe pushed against that side would get, which is how you stop a machine that sorts by side from taking an ingredient in the wrong slot.

<Color id="yellow">Priority</Color> decides which link is filled first when more than one would accept the same thing; higher goes first, and links that tie keep the order they are listed in. Hold <Color id="yellow">Shift</Color> to step by ten. This is what puts a fussy hatch ahead of a general one.

<Color id="yellow">Insert filter</Color> governs what may be pushed into the link, and <Color id="yellow">return filter</Color> what may be pulled back out of it. Each holds nine entries and each has its own <Color id="green">Allow</Color> or <Color id="red">Deny</Color> mode. A filter with nothing in it allows everything, in either mode, so an empty Deny list denies nothing.

Left click a filter slot with something on your cursor to set it, and left click it empty-handed or right click it to clear it. With JEI installed you can drag straight out of the ingredient list instead, without hunting the item down first. Fluids work either way, dragged from JEI or carried out of a terminal. A filter a link's role never consults, an insert filter on an Output link say, is greyed out rather than hidden, and refuses a drag.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What It Does Not Do</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Hub moves items and fluids and nothing else. It does not know the machine's recipes, does not check that the structure is built, does not supply power, and does not wait for a craft to finish. The machine runs itself exactly as it would if you had piped everything in by hand.

<Color id="red">It is a real device on the network.</Color> The Hub sits on the ME network like any machine: it needs power, and it takes up channels. Eleven for the Hub itself, plus one more for each inventory it is linked to, standing in for the pattern provider every hatch would otherwise have needed. That is already past what a normal cable carries before a single link is counted, so a Hub needs a dense cable and an ME Controller behind it. A Hub that cannot claim all of them goes offline and stops offering storage entirely, and its screen says so.

<Color id="red">It also cannot invent an inventory that is not there.</Color> If a machine's parts expose nothing to the outside world, there is nothing for the Hub to link, and no Provider of any kind was ever going to reach it.