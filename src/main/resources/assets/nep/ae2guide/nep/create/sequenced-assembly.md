---
navigation:
  title: Sequenced Assembly
  icon: nep:sequenced_assembly_controller
  parent: nep/create/index.md
  position: 64
item_ids:
  - nep:sequenced_assembly_controller
  - nep:sequenced_assembly_linker
  - nep:sequenced_assembly_pattern
---
# <Color id="gold">Sequenced Assembly</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Sequenced Assembly</Color>

  <ItemImage id="nep:sequenced_assembly_controller" scale="2"/>

  Automate `create:sequenced_assembly` on a real belt line. The Controller is a black box around the whole line: one pattern goes in, one finished item comes out. It feeds every station, injects the base item, and recirculates the part until it is done.
</Column>

<Recipe id="nep:module_status/sequenced_assembly"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="nep:sequenced_assembly_controller" x="0" y="0" z="0"/>
  <Block id="ae2:pattern_provider" x="0" y="1" z="0"/>

  <BlockAnnotation x="0" y="1" z="0" color="#00ccff">
    **Pattern Provider**, on the Controller
  </BlockAnnotation>
  <BlockAnnotation x="0" y="0" z="0" color="#ffaa00">
    **Controller**, placed anywhere near the line
  </BlockAnnotation>
</GameScene>

The line itself is an ordinary Create build. It does not have to be a closed loop, because the Controller carries parts back to the start in code.

* An **input** belt at the head of the line.
* Your **stations** over that belt. Deployers must face down, and every station needs rotational power.
* A **Depot** at the end as the output. This one is mandatory.
* The **Pattern Provider** goes on the Controller, not on the Depot.

Results are pushed straight back into the Provider, so no import bus is needed anywhere.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Teaching It The Layout</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Controller cannot see your build on its own. The <ItemLink id="nep:sequenced_assembly_linker"/> tells it what is what.

| Mode | Use it on |
|---|---|
| Set Input | The belt at the head of the line |
| Set Output | The Depot at the end |
| Set Machine | Each station, in recipe step order |

The plan is stored on the Linker as you tag blocks, then applied when you use the Linker on the Controller. Sneak and use cycles the mode, and sneak and use in the air clears the plan.

> <Color id="yellow">Tag your stations in recipe step order.</Color> Each Deployer is assigned the tool for its own step by that order, so tagging them out of order sends the wrong item to the wrong Deployer.

Everything must sit within the configured link range of the Controller, which defaults to 16 blocks.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A <ItemLink id="nep:sequenced_assembly_pattern"/>, and <Color id="red">only</Color> that. Look the recipe up in JEI, open a <ItemLink id="ae2:pattern_encoding_terminal"/>, and click the **+** on the recipe: the blank pattern comes back as a Sequenced Assembly Pattern. The wireless encoding terminal works the same way.

The pattern records the recipe itself, so it names the exact `create:sequenced_assembly` recipe to run rather than leaving the Controller to guess one from the result. Two recipes that produce the same item no longer collide, and the inputs are worked out for you: the base item plus every deployed item and fluid the recipe consumes, <Color id="red">summed over every loop</Color>. A five loop recipe that deploys one cogwheel per loop asks for five cogwheels, and you never do that arithmetic yourself.

> <Color id="yellow">A plain processing pattern is not accepted.</Color> The Controller ignores it and nothing is pushed, so any processing pattern you already had has to be re-encoded.

Without JEI you can still encode one by hand. Set the terminal to processing mode, fill in the base item plus every deployed item and fluid at their <Color id="red">summed</Color> totals, put the finished item in the result, and encode: if those ingredients fit exactly one `create:sequenced_assembly` recipe, the pattern comes back as a Sequenced Assembly Pattern regardless. If the counts are off, or two recipes both fit, the terminal tells you and hands back an ordinary processing pattern, which the Controller will not take. That last case is the one the **+** button exists for.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">By Hand</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Controller does not have to be driven by a pattern. Open it and place the base item along with every deployed tool straight into its input grid, and it works out the recipe and runs the line just as it would for the network. Finished items gather in its output slots for you to collect instead of going back to a Provider, so a line fed by hand needs no Pattern Provider at all. Fluids can be staged the same way; see Staging Tanks below.

> <Color id="yellow">Best kept to recipes with a single, certain result.</Color> A chance recipe run by hand has nowhere to bank its junk, so feed the Controller yourself only for deterministic recipes.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Chance Recipes</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Many sequenced assembly recipes can roll junk instead of the item you wanted. The Controller absorbs that for you. It keeps the line running until the real result appears, so the network only ever sees the one item it asked for. Junk is placed into network storage as ordinary stock, and fresh ingredients are requested to replace the wasted attempt.

If the network runs dry, the Controller halts and its face turns <Color id="red">red</Color>. Hover the status line in its interface to see exactly which material is missing, items and Spout fluids alike, with the amount each step needs. A Controller reading <Color id="red">Output blocked</Color> has finished an item that fits neither its output buffer nor network storage, so the line is jammed on the output Depot until you free space. Set its Comparator to Status mode and it emits a full signal while halted or blocked, so an alarm can be wired to it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Hold the finished item and use it on the Controller. It prints a full report of what the recipe needs against what your line actually has.
* Every Deployer must face down, and every station needs rotational power.
* The output must be a <ItemLink id="create:depot"/>. Nothing else is accepted.
* Open the Controller to see its station strip. A red bar under a station means that station is the problem.
* A job that can never finish, because you cancelled it in the terminal or the recipe changed, can be dropped with the <Color id="red">✗</Color> button at the top right of the interface. It forgets every craft the Controller still owes and cancels the ingredients it has on request. Anything already finished is still handed back.
* Dropping a job, or emptying the buffers into your hands, leaves the parts already travelling down the belt with nowhere to go. The Controller keeps clearing its Depot for ten seconds afterwards and places whatever arrives into network storage, and the clock restarts with each part it collects, so a whole line's worth still comes home. Tune the window with `reclaimGrace`.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Staging Tanks</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Fluids a pattern sends arrive before the Spouts need them, so the Controller stages them in four tanks beside its ingredient grid. Each holds <Color id="aqua">64,000 mB</Color> and one fluid claims one tank, so a recipe may use up to four different fluids at once. The Controller tops the Spouts up from them as the line runs. Hover a tank to see what it holds.

You can also fill the tanks by hand. Use a filled bucket or fluid container on the Controller, or click one onto a tank while the interface is open, and its contents empty into a matching or empty tank.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Comparator</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A Comparator beside the Controller can read one of three things. The signal button at the top of the interface cycles between them, and its letter shows which is active.

| Mode | What it reads |
|---|---|
| Output | How full the output buffer is |
| Status | Zero when idle, rising with the number of crafts still owed, and a full 15 while the line is halted for want of materials |
| Input | How full the ingredient grid and the staging tanks are together |