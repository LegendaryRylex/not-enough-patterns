---
navigation:
  title: Ritual Conductor
  icon: nep:ritual_conductor
  parent: nep/ars_nouveau/index.md
  position: 40
item_ids:
  - nep:ritual_conductor
---
# <Color id="gold">Ritual Conductor</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Ritual Conductor</Color>

  <ItemImage id="nep:ritual_conductor" scale="2"/>

  Almost every ritual is a world effect rather than a craft, so there is no result for a pattern to name. The Conductor automates the part that can be automated: keeping a Brazier supplied and lighting it.
</Column>

<Recipe id="nep:module_status/ritual_conductor"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="ars_nouveau:ritual_brazier" x="1" y="0"/>
  <Block id="nep:ritual_conductor" y="0"/>
  <BlockAnnotation y="0" color="#88cc00">
    **Ritual Conductor**, within four blocks of the Brazier
  </BlockAnnotation>
  <BlockAnnotation x="1" y="0" color="#ffaa00">
    **Ritual Brazier**, the thing being driven
  </BlockAnnotation>
</GameScene>

* Wire the Conductor into an ME network and place it within four blocks of a Ritual Brazier. The nearest Brazier it can see is the one it drives.
* **Right click it to open its screen**, empty handed or not. Everything the Conductor does is configured there, so there is nothing to memorise and nothing to get wrong by holding the wrong item.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The screen is the whole configuration in one place, so a Conductor never has to be guessed at.

* The list on the left holds **every ritual registered on the server**, addon rituals included. Click one to conduct it, click it again to stop. Hovering a row gives its description and its source cost per run.
* The chosen ritual is named in full at the top of the right side, with its source cost per run.
* **Repeat** is how long the Conductor waits after one ritual ends before it starts the next. Left click the arrows to step by five seconds, right click to step by a minute. At its lowest setting it re-arms as soon as it can.
* **Weather** and **Time** hold the next run back until the world agrees: clear sky, rain, thunder, day or night. Click a value or its arrows to cycle it, right click to cycle back. A ritual already burning is never interrupted; only the next one waits.
* **Output** decides whether the Conductor collects what the ritual leaves behind or leaves it be.
* The twelve **augment slots** are ghost slots. Pick an item up from your inventory and click a slot to have that item fed on every run; click a filled slot with an empty cursor to clear it. Nothing is ever taken from you, the slot only holds a template. Fill two slots with the same item to feed two of them.

The line at the top says what it is doing right now, and its right end names the Brazier it has locked on to.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Loop</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Once a second the Conductor looks at its Brazier and does whichever of these applies:

| Brazier state | What the Conductor does |
|---|---|
| Empty, conditions unmet | Holds, and counts down to the next run |
| Empty, conditions met | Draws one matching tablet out of the network and sets the ritual |
| Set, not yet lit | Feeds each configured augment in turn, then lights it once the ritual will start |
| Running | Waits |

When the ritual ends the Brazier empties itself, and the loop begins again with a fresh tablet.

> <Color id="yellow">Every run costs a tablet.</Color> That is Ars Nouveau's own economy, not something NEP adds, so continuous ritual automation means automating tablet production too.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Redstone and Comparators</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A redstone signal on the Conductor pauses it. It keeps watching, but arms nothing and lights nothing, which is how you gate a ritual on a timer or a lever.

A comparator reads what it is doing:

| Signal | Meaning |
|---|---|
| 0 | No ritual set, or no Brazier in range |
| 2 | Holding for its weather, time or repeat setting, or paused by redstone |
| 5 | Waiting for a tablet to arrive in the network |
| 10 | Armed, feeding augments or waiting on the ritual's own conditions, or the running ritual is out of source |
| 15 | The ritual is running |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Source</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Conductor supplies no source. A running ritual pulls its own from Source Jars within six blocks of the **Brazier**, and simply stalls without them. Build the jars around the Brazier, not around the Conductor.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Collecting The Output</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

With **Output** set to Collect, the Conductor takes what the ritual leaves behind into the network by two routes:

* It behaves as an ordinary inventory, so a ritual that hands its yield to the Brazier's neighbours reaches the network directly. Harvest does this. Place the Conductor against the Brazier to catch it.
* When a ritual finishes, it sweeps up what was dropped around the Brazier. Dig, Disintegration and Awakening all drop their results on the ground.

> <Color id="yellow">The sweep runs once per completed ritual</Color>, not on a timer. Rituals vary in length and stall without source, so a Conductor that polled for loose items would spend most of its life scanning for nothing. Waiting for the ritual to actually end costs one scan per result instead.

Items a player threw are never swept, so a Conductor cannot steal what you drop next to the Brazier. `Collection Radius` in the config sets how far the sweep reaches, and 0 turns it off entirely.
