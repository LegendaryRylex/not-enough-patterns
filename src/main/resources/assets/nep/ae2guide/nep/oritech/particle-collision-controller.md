---
navigation:
  title: Particle Collision Controller
  icon: nep:particle_collision_controller
  parent: nep/oritech/index.md
  position: 20
item_ids:
  - nep:particle_collision_controller
  - nep:accelerator_linker
---
# <Color id="gold">Particle Collision Controller</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Particle Collision Controller</Color>

  <ItemImage id="nep:particle_collision_controller" scale="2"/>

  Runs particle collision patterns on a Particle Accelerator you built. It speeds one particle up around the ring, then releases the second into its path at the right moment, and collects whatever the collision makes.
</Column>

<Recipe id="nep:module_status/particle_collision_controller"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Build the ring the way Oritech's own guide describes: guide rings, powered motors, and two <ItemLink id="oritech:accelerator_controller"/>s feeding particles into **the same ring**.
* Take an <ItemLink id="nep:accelerator_linker"/> and use it on the two Accelerator Controllers. The first one you link launches the fast particle, the second one the slow particle. Using it on a linked controller again un-links it.
* Use the linker on the Particle Collision Controller to sync. Both Accelerator Controllers must be within <nep:ConfigValue name="particleCollisionControllerLinkRange"/> blocks of it. Sneak and use it on the Controller to drop its links.
* Put a Pattern Provider against the Controller and fill it with <ItemLink id="nep:particle_collision_pattern"/>s. The Controller is an ME machine: it joins the network through its own grid connection and returns finished items to the Provider on its own.

<Recipe id="nep:accelerator_linker"/>

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Top Speed</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A collision below the recipe's speed destroys both items, and a particle that goes too fast for a bend flies out of the ring and is lost. So the Controller remembers the fastest particle it has seen in the linked ring, and only takes patterns that need no more than that.

* A freshly linked ring reads **Unmeasured** and refuses every pattern. Drop any cheap item into either Accelerator Controller by hand and let it run until it flies out: the Controller watches it and records its speed.
* Particles from its own collisions count too, so the record only ever climbs while the ring keeps up.
* If a particle is lost mid-job, the Controller lowers the record below that recipe so it stops trying. Fix the ring, then fire another particle through it to measure again.
* Linking new Accelerator Controllers starts the record over.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Magnetic Field</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

With **Oritech Things** installed, an Accelerator Magnetic Field linked to one of the two Accelerator Controllers carries that controller's particle through every bend, so the ring has no top speed to measure.

* While the field has energy and no redstone signal, the Controller takes every pattern, whatever its speed, and the readout shows **Magnetic field** in place of the top speed.
* The fast particle always launches from the Accelerator Controller the field is linked to, whichever one you linked first.
* A particle lost while the field holds, to a field that ran dry or to motors that stopped pushing, caps the field at that speed, so the Controller stops retrying a recipe it cannot finish. The readout then shows **Field, top** and the cap. A faster particle lifts it, and relinking clears it.
* Skip the measuring particle on a field ring. The field holds it in the ring for good, and the Controller reads **Accelerator busy** until a redstone signal on the field lets it fly out.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How It Crafts</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.35"/>

1) The Controller waits until both Accelerator Controllers are empty, with no particle in flight and no result waiting, then loads the first item into the fast one.

2) The bar tracks that particle's speed. The collision's energy is the sum of both speeds, and the slow particle starts at 1, so the Controller waits for the recipe's speed minus one.

3) It loads the second item into the slow Accelerator Controller. The fast particle catches it within a lap.

4) The result appears in whichever Accelerator Controller caught the collision, and the Controller pulls it into its output buffer.

If the slow particle never launches, or the two never meet before the collision timeout, the Controller recalls both particles and puts their items back in its buffer. It waits ten seconds before trying again.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The top readout names the current stage and the ring's recorded top speed. While a job runs, the third line shows the fast particle's speed against the speed it needs.
* The input buffer only accepts items a pending collision still needs.
* **Clear Pending Recipes** drops every collision the Controller still owes and cancels the crafting jobs on the network that were waiting on it.
* **Empty Buffers** returns everything staged to your inventory.
* A Comparator beside the block reads the output buffer.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **Refused: ring too slow.** No particle has reached the pattern's speed in the linked ring. Speed the ring up, then measure it again.
* **Accelerator busy.** Something else put an item in a linked Accelerator Controller, or left a result in it. Keep hoppers and pipes away from both.
* **Particle never launched.** An Accelerator Controller needs a straight guide ring right behind it, facing to the side.
* **Particle not speeding up.** The fast particle has gone a while without gaining speed. Check the motors have power.
* **Particles never met.** Both particles flew but missed each other. Link two Accelerator Controllers on the same ring.
* The Controller must be on a powered ME network, and it takes up <nep:ConfigValue name="particleCollisionControllerChannels"/> channels.
* Turn on Verbose Logging under Debug to have the Controller log every launch, collision and lost particle.
