---
navigation:
  title: Ritual Controller
  icon: nep:ritual_controller
  parent: nep/occultism/index.md
  position: 30
item_ids:
  - nep:ritual_controller
  - nep:ritual_pattern
---
# <Color id="gold">Ritual Controller</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Ritual Controller</Color>

  <ItemImage id="nep:ritual_controller" scale="2"/>

  Runs Occultism's crafting rituals for ME auto-crafting on a pentacle you drew. It lays the ingredients into the sacrificial bowls, starts the ritual at the golden bowl, and sends the result back to the Pattern Provider that ordered it.
</Column>

<Recipe id="nep:module_status/ritual_controller"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Obtaining</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Controller is bound in a ritual on Occultism's foliot crafting pentacle, started with a bound Book of Binding for a foliot. The bowls hold an ME Pattern Provider, a Golden Sacrificial Bowl, an Engineering Processor, a Spirit Attuned Gem and two silver ingots. The recipe viewer shows the ritual in full.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Draw the pentacle the way Occultism's own book describes, with its golden bowl in the middle and enough sacrificial bowls around it for the rituals you want.
* Place the Controller within <nep:ConfigValue name="ritualControllerBowlRange"/> blocks of the golden bowl. The nearest golden bowl it can see is the one it drives, and the third readout line shows where it is.
* Put a [Spirit-Infused Book](/nep/occultism/spirit-infused-books.md) in the book slot on the right. Any job will do; only the spirit's tier matters here.
* Put a Pattern Provider against the Controller and fill it with <ItemLink id="nep:ritual_pattern"/>s. The Controller is an ME machine: it joins the network through its own grid connection and takes up <nep:ConfigValue name="ritualControllerChannels"/> channels.
* Keep every sacrificial bowl in the pentacle empty. The Controller only starts a ritual when it can lay out all of the ingredients itself.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Bound Spirit</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The book's spirit decides which pentacles the Controller may use, and how fast its rituals go.

| Spirit | Pentacles | Speed |
|---|---|---|
| Foliot | Foliot crafting | Normal |
| Djinni | Up to djinni crafting, and Osorin's Unbound Calling | Faster |
| Afrit | Up to afrit crafting | Faster still |
| Marid | Every pentacle, Ronaza's Contact included | Fastest |

Each tier above foliot adds <nep:ConfigValue name="ritualControllerTierSpeed"/> percent to the ritual's pace. A pattern for a stronger pentacle than the book allows is refused.

The ritual still spends its own Book of Binding, as it does when you start it by hand. That book is one of the pattern's ingredients.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Ritual Patterns</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Open a Pattern Encoding Terminal, pick a ritual in JEI and press **+**. Crafting, spirit-named, miner spirit and upgrade rituals all encode. Summoning, possession and familiar rituals have no item to make and stay out.
* The pattern's ingredients are the activation item and everything that goes in the bowls.
* A bound Book of Binding carries a random spirit name. A pattern accepts any book of the right kind, whatever its name, and AE2 jobs waiting on a named item, such as a satchel or a miner spirit, accept it under any name too.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Sacrifices</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Some rituals want a creature slain nearby while they run. The Controller cannot fight, so it spends a stand-in item instead, which the pattern carries as one more ingredient. JEI offers the stand-ins when you encode the pattern, and picks the one your network holds most of.

* **A soul gem** holding a creature the ritual accepts. A soul gem comes back empty to the network afterwards; a fragile soul gem breaks.
* **A spawn egg** of such a creature.
* **A Mystical Agriculture essence** of that creature, where one exists: pig, sheep, armadillo and enderman.

Which items stand in for which creatures is data: each `nep:sacrifice_stand_in` recipe names a creature tag and an item ingredient, and nep ships one per sacrifice Occultism uses, each reading its own item tag under `nep:sacrifice_stand_ins/`. A datapack can add to those tags, or add recipes for creature tags of its own.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Repairs</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Turn on **Repair Network Items** in the screen, and while the Controller has no pattern work it repairs damaged items stored on its network with Occultism's repair rituals: tools, armour, chalks and miner spirits.

* It looks for one damaged item and the ritual's other ingredients, takes them out of storage, runs the repair, and puts the mended item back.
* Repairs use the same pentacle rules as patterns, so a repair on the afrit pentacle needs an afrit or marid book.
* Pattern work always goes first.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The top readout names what the Controller is doing and the bound spirit's tier. The second line shows the ritual in progress and how many more are queued, and the third shows the golden bowl and whether repairs are on.
* The bar tracks the ritual's time.
* The input buffer only accepts items a queued ritual still needs.
* **Clear Pending Recipes** drops every ritual the Controller still owes and cancels the crafting jobs on the network that were waiting on it.
* **Empty Buffers** returns everything staged to your inventory.
* A Comparator beside the block reads the output buffer.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **Wrong pentacle.** The chalk around the golden bowl is not the pentacle this ritual needs. Occultism's book shows each one.
* **Sacrificial bowls in use.** A bowl still holds something. The Controller never lays ingredients beside items it did not put there.
* **Not enough sacrificial bowls.** The ritual needs one bowl per ingredient within eight blocks of the golden bowl.
* **Spirit too weak.** Bind a stronger spirit's book into the Controller.
* **Ritual interrupted.** Something broke the ritual partway, often a disturbed pentacle. The Controller gathers up what the golden bowl dropped, puts it back in its buffer, and tries again after a short wait.
* The Controller must be on a powered ME network.
* Turn on Verbose Logging under Debug to have the Controller log every ritual it starts, finishes or loses.
