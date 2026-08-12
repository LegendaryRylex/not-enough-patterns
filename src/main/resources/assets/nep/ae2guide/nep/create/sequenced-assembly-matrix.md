---
navigation:
  title: Assembly Matrix
  icon: nep:sequenced_assembly_matrix
  parent: nep/create/index.md
  position: 65
item_ids:
  - nep:sequenced_assembly_matrix
  - nep:matrix_circuitry
  - nep:hardened_obsidian_plate
---
# <Color id="gold">Assembly Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Assembly Matrix</Color>

  <ItemImage id="nep:sequenced_assembly_matrix" scale="2"/>

  One block that replaces an entire sequenced assembly line. No belt, no Deployers, no Depot. It runs the whole recipe internally and, out of the box, <Color id="green">always</Color> yields the primary result, because the chance roll never happens.
</Column>

<Recipe id="nep:module_status/sequenced_assembly_matrix"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="create:shaft" x="0" y="0" z="0" p:axis="x"/>
  <Block id="nep:sequenced_assembly_matrix" x="1" y="0" z="0" p:axis="x"/>
  <Block id="create:shaft" x="2" y="0" z="0" p:axis="x"/>
  <Block id="ae2:pattern_provider" x="1" y="1" z="0"/>

  <BlockAnnotation x="1" y="1" z="0" color="#00ccff">
    **Pattern Provider**, on any face
  </BlockAnnotation>
  <BlockAnnotation x="1" y="0" z="0" color="#ffaa00">
    **Matrix**, shaft on either end of its axis
  </BlockAnnotation>
</GameScene>

* Rotational power on either end of the axis it was placed along. The four side faces transmit nothing, so a cogwheel there does nothing at all. Placing the Matrix against an existing shaft snaps it to that axis, and a <ItemLink id="create:wrench"/> turns it afterwards without disturbing anything inside it. Sneak and wrench to pick it up.
* A connection to your ME network for power. It idles at <Color id="aqua">10 AE/t</Color> and draws <Color id="aqua">500 AE/t</Color> while assembling. The readout shows which it is doing.
* With Create: New Age installed, recipes with an energising step also cost Forge Energy. The Matrix grows an internal FE buffer for exactly that case; wire any FE source to any face and it charges at up to 100,000 FE/t. Recipes without an energising step never touch it.
* A Pattern Provider on any face. Every craft begins with a pattern pushed to the Matrix, so without one it sits idle no matter what you put inside it.
* A Comparator beside it can read the output buffer, the staged input, or the machine's own status. See Comparator below.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Speed Scales With Stress</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Matrix draws more stress the faster its shaft turns, and the more stress it draws, the faster it assembles. Below <Color id="red">32 RPM</Color> it does nothing at all and costs no stress.

| Shaft speed | Stress drawn | Time per item |
|---|---|---|
| Under 32 RPM | none | does not run |
| 32 RPM | 8,192 SU | 9 seconds |
| 64 RPM | 49,152 SU | 1.5 seconds |
| 128 RPM | 131,072 SU | 0.6 seconds |
| 256 RPM | 294,912 SU | 0.25 seconds |

> <Color id="yellow">Gearing up pays off far more than it looks.</Color> Doubling the shaft from 32 RPM to 64 RPM is six times the throughput, not twice. Its interface shows operating speed and processing speed side by side so you can see exactly what your gearing is buying.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What It Will Craft</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A recipe runs the moment everything it consumes is sitting inside the Matrix at once. There are no steps to schedule and no part travelling between stations, so it needs the whole shopping list up front: the base item once, plus every deployed item and fluid multiplied by the number of loops.

A Precision Mechanism, which loops five times over three deploying steps, needs one Golden Sheet, five Cogwheels, five Large Cogwheels, and five Iron Nuggets. Put those in and one Precision Mechanism comes out, every time.

Every sequenced assembly job starts with a <ItemLink id="nep:sequenced_assembly_pattern"/>. A Matrix that owes the network nothing refuses items at every face and in its own interface, so it can never sit on loose ingredients wondering what they were for. Obsidian Dust alone could be headed for a Sturdy Sheet or a <ItemLink id="hardened_obsidian_plate"/>, and a pattern is what settles the question. Encode one by looking the recipe up in JEI and clicking the **+** on it in a <ItemLink id="ae2:pattern_encoding_terminal"/>; the pattern names the recipe outright, so it settles that question even when two recipes end in the same item. Encoding the ingredients by hand in processing mode works too, and comes back as a Sequenced Assembly Pattern whenever they fit exactly one recipe.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Filling And Deploying</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The same steps the Matrix runs inside a sequenced assembly recipe are offered on their own. Push it a <Color id="aqua">Filling</Color> or <Color id="aqua">Deploying</Color> pattern and it runs that single recipe, so a build that already has a Matrix needs no Spout over a Depot for [Filling](/nep/create/filling.md) and no Deployer for [Deploying](/nep/create/deploying.md). Those patterns are the ordinary ones, encoded exactly as they are for a Depot, and each craft costs the same speed and stress as an assembly craft.

* Recipes that keep their tool, such as stripping a log with an axe, work if the pattern hands the Matrix the tool. Create never wears a kept tool down, so the same axe is handed straight back to the Pattern Provider with the result.
* Recipes whose tool a Deployer <Color id="red">damages</Color> rather than consumes are refused, because a pattern cannot name an item that comes back with a point of durability missing. Build the Depot and Deployer for those.
* An item the Matrix is already assembling cannot also be filled or deployed until that job finishes, since one Matrix runs one recipe per result at a time.

Turning off the Filling or Deploying module turns it off in the Matrix too; there is no separate switch.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Helping A Job Along</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Once a job is underway you can help it along. The Matrix accepts hoppers, pipes and items placed by hand, but only up to what the job it already owes is still short of, and only for items that job actually uses. Offer a stack of sixty-four when it needs one and it takes the one. See If Nothing Happens below for reading what it is short of.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Chance Recipes</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

On a physical line, a recipe like the Precision Mechanism rolls its result pool at the end and often hands you junk instead. The Matrix skips that roll entirely, which is the point of building one. Those ingredient counts above are the true cost per item, not per attempt.

Packs that consider this too generous can turn <Color id="aqua">Guaranteed Results</Color> off. The Matrix then rolls exactly as a line does, and every attempt costs a full set of ingredients whether it succeeds or not.

With the roll switched on:

* Junk goes <Color id="aqua">straight into network storage</Color> as ordinary stock, so the output buffer never clogs with it. Only the item you actually asked for is handed back to the Pattern Provider.
* An attempt that misses leaves the network still waiting, so the Matrix requests a fresh set of ingredients and tries again. It keeps going until the real result appears.
* Turn <Color id="aqua">Auto-Request Recipes</Color> off and it will not do that. An unlucky roll then leaves the craft sitting there until you restock the Matrix yourself.

> <Color id="yellow">Leave Guaranteed Results on if you are unsure.</Color> It is what makes the Matrix worth its recipe, and it is the only mode where the ingredient cost of a craft is predictable.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Fluids</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Four internal tanks sit beside the input grid, so a recipe may use up to four different fluids. Each holds 64,000 mB. The network fills them for you when a pattern includes fluid inputs, which is the only way they are filled. Hover a tank to see what it holds.

> <Color id="yellow">One fluid per tank.</Color> A fluid claims exactly one tank and never spills into a second, so a full tank rejects the rest rather than eating a slot another fluid needs.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Comparator</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A Comparator beside the Matrix reads one of three things. The signal button at the top of the interface cycles between them, and its letter shows which is active.

| Mode | What it reads |
|---|---|
| Output | How full the output buffer is |
| Status | Zero when idle, climbing as the current craft progresses, and a full 15 when it has stalled for want of speed, stress, or power |
| Input | How full the input grid and the tanks are together |

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The status line at the top names what the Matrix is doing, or what is stopping it. Hover it whenever it turns red; the tooltip carries the detail, including the exact list of materials a starved craft is short of.
* **Crafting** lists what the Matrix owes right now, each with its count. It reads *Nothing* when the Matrix has been asked for nothing, which is not the same as being unable to run.
* The readouts beside it show the AE/t being drawn, the current RPM against Create's maximum, and the stress being taken.
* The input grid stages the items a pending craft needs, and the output buffer holds what is finished. Neither accepts items a pending craft does not use.
* The four tanks sit beside the grid. Hover one to see what it holds against its capacity, or click it holding a filled bucket or tank to pour that fluid in by hand.
* <Color id="red">✗</Color> **Clear Pending Recipes** drops every craft the Matrix still owes, cancels ingredients it has on request, and cancels the crafting jobs waiting on it. Items already made are still returned. It greys out when nothing is pending.
* **Comparator Signal** cycles what the comparator beside the Matrix emits, and its letter shows which mode is live.
* <Color id="aqua">↓</Color> **Empty Buffers** returns staged ingredients and finished items to your inventory, and sends staged fluids back to the network.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Building One</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Matrix is itself a sequenced assembly recipe, so the first one has to be built on a real line. See <ItemLink id="nep:sequenced_assembly_controller"/> for that. Afterwards a Matrix can build more of itself.

<Row>
  <ItemImage id="nep:hardened_obsidian_plate"/>
  ### <Color id="aqua">Hardened Obsidian Plate</Color>
</Row>

Starts from obsidian dust and does not loop.

<RecipeFor id="nep:hardened_obsidian_plate" fallbackText="Press, Spout 500 mB Lava, Spout 500 mB Water, then Press twice more."/>

<ItemImage id="minecraft:air" scale="0.5"/>

<Row>
  <ItemImage id="nep:matrix_circuitry"/>
  ### <Color id="aqua">Matrix Circuitry</Color>
</Row>

The universal upgrade component shared by every Matrix, built entirely from ME parts in a regular crafting table.

<RecipeFor id="nep:matrix_circuitry" fallbackText="A shaped crafting recipe of processors, crafting accelerators, a 256k cell component, and a singularity."/>

<ItemImage id="minecraft:air" scale="0.5"/>

<Row>
  <ItemImage id="nep:sequenced_assembly_matrix"/>
  ### <Color id="gold">Assembly Matrix</Color>
</Row>

Starts from a <ItemLink id="nep:sequenced_assembly_controller"/> and loops <Color id="red">four</Color> times, so multiply every deployed item by four when you encode the pattern. That means four each of the Matrix Circuitry, Printed Silicon, and Hardened Obsidian Plate.

<RecipeFor id="nep:sequenced_assembly_matrix" fallbackText="Saw, deploy Matrix Circuitry, deploy Printed Silicon, deploy Hardened Obsidian Plate, then Press. Four loops."/>

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Open the Matrix. Its status line names the problem directly.

* <Color id="red">Too slow</Color> means the shaft is turning below the minimum speed.
* <Color id="red">Overstressed</Color> means the kinetic network cannot carry it. Add capacity.
* <Color id="red">No network power</Color> means the ME network cannot supply the 500 AE/t an assembling Matrix needs.
* <Color id="gray">Idle</Color> with items inside means no recipe is fully satisfied yet. Check the count of every deployed ingredient against the loop count. It also means no pattern has been pushed, since the Matrix never starts a craft it was not asked for.
* <Color id="red">Missing materials</Color> means a craft is still owed but the network is not supplying what it needs. Hover the status line to see exactly which materials are short and by how much. Those are precisely the items the Matrix will accept from you, so you can drop them in yourself rather than wait for the network. This is what you see after an attempt rolls junk and takes its ingredients with it, only possible with Guaranteed Results off: the Matrix asks for a fresh set and reports anything the network has neither in stock nor a pattern for. With Auto-Request Recipes off it lists whatever the recipe still needs, for you to restock by hand.
* <Color id="red">Needs energy</Color> means the next craft has an energising step (Create: New Age) and the FE buffer holds less than it costs. The craft starts, and charges the whole cost, only once the buffer covers it; hover the status line to see stored against needed. Feed the Matrix Forge Energy through any face.
* <Color id="red">Output blocked</Color> means the finished item fits neither the output buffer nor network storage, so the craft is held at full progress. Free a slot, or give the Matrix somewhere to push to.
* A job that can never finish, because you cancelled it in the terminal or the recipe changed, can be dropped with the <Color id="red">✗</Color> button at the top right of the interface. It forgets every craft the Matrix still owes and cancels the ingredients it has on request. Anything already assembled is still handed back.