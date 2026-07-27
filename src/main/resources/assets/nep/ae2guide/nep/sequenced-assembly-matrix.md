---
navigation:
  title: Sequenced Assembly Matrix
  icon: nep:sequenced_assembly_matrix
  parent: nep/nep-index.md
  position: 70
item_ids:
  - nep:sequenced_assembly_matrix
  - nep:matrix_circuitry
  - nep:hardened_obsidian_plate
  - nep:sequenced_assembly_pattern
---
# <Color id="aqua">Sequenced Assembly Matrix</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Sequenced Assembly Matrix</Color>

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

Every job starts with a pattern, and it must be a <ItemLink id="nep:sequenced_assembly_pattern"/>. A Matrix that owes the network nothing refuses items at every face and in its own interface, so it can never sit on loose ingredients wondering what they were for. Obsidian Dust alone could be headed for a Sturdy Sheet or a <ItemLink id="hardened_obsidian_plate"/>, and a pattern is what settles the question. Encode one by looking the recipe up in JEI and clicking the **+** on it in a <ItemLink id="ae2:pattern_encoding_terminal"/>; the pattern names the recipe outright, so it settles that question even when two recipes end in the same item. Encoding the ingredients by hand in processing mode works too, and comes back as a Sequenced Assembly Pattern whenever they fit exactly one recipe. A plain processing pattern is <Color id="red">not</Color> accepted and is never pushed.

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
  ### <Color id="aqua">Sequenced Assembly Matrix</Color>
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
* <Color id="red">Output blocked</Color> means the finished item fits neither the output buffer nor network storage, so the craft is held at full progress. Free a slot, or give the Matrix somewhere to push to.
* A job that can never finish, because you cancelled it in the terminal or the recipe changed, can be dropped with the <Color id="red">✗</Color> button at the top right of the interface. It forgets every craft the Matrix still owes and cancels the ingredients it has on request. Anything already assembled is still handed back.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Module Settings</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Under Modules → Create → Sequenced Assembly Matrix.

| Setting | Default | Effect |
|---|---|---|
| Enabled | true | Turns the Matrix on or off |
| Guaranteed Results | true | Skips recipe output chances, so every craft yields the primary result |
| Auto-Request Recipes | true | Lets the Matrix pull replacement ingredients when a craft it owes runs dry |
| Power Usage | 500 | AE per tick drawn while assembling |
| Idle Power Usage | 10 | AE per tick drawn while idle |
| Minimum Speed | 32 | RPM needed before it runs at all |
| Minimum Stress Units | 8,192 | Stress drawn at that minimum speed |
| Maximum Stress Units | 294,912 | Stress drawn at Create's maximum speed |
| Craft Ticks At Maximum Stress | 5 | Ticks per item while drawing maximum stress |
| Tank Capacity | 64,000 | Millibuckets held by each of the four tanks |

With the module off, the Matrix stops accepting patterns and stops assembling, though it still returns anything it already owes the network. See [Overview](getting-started.md) for where the config lives and for the master switch.

> <Color id="yellow">`idlePowerUsage` is read when the Matrix first joins a network.</Color> Change it and the Matrix needs a world reload before the new figure applies. `powerUsage` takes effect immediately.
