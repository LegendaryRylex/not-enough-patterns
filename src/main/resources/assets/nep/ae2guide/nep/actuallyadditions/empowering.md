---
navigation:
  title: Empowering
  icon: actuallyadditions:empowerer
  parent: nep/actuallyadditions/index.md
  position: 32
item_ids:
  - nep:empowering_pattern
---
# <Color id="gold">Empowering</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Empowering</Color>

  <ItemImage id="nep:empowering_pattern" scale="2"/>

  A pattern provider against an Empowerer stages every item at once: the base item goes into the Empowerer, and one extra item onto each of the four Display Stands standing three blocks out.
</Column>

<Recipe id="nep:module_status/empowering"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="4">
  <Block id="actuallyadditions:empowerer" x="0" y="1" z="0"/>
  <Block id="ae2:pattern_provider" x="0" y="0" z="0"/>
  <Block id="actuallyadditions:display_stand" x="3" y="1" z="0"/>
  <Block id="actuallyadditions:display_stand" x="-3" y="1" z="0"/>
  <Block id="actuallyadditions:display_stand" x="0" y="1" z="3"/>
  <Block id="actuallyadditions:display_stand" x="0" y="1" z="-3"/>
</GameScene>

* Build the Empowerer exactly as Actually Additions wants it: four Display Stands, three blocks away on each horizontal axis, all at the same height.
* Put a Pattern Provider against any face of the **Empowerer** and drop <ItemLink id="nep:empowering_pattern"/> patterns in it.
* Keep the four stands powered. They are still the only source of energy the craft has, and the Empowerer itself holds none.
* Add an <ItemLink id="nep:import_card"/> to the Provider if you wish the crafted item to be returned directly to the system once the craft is finished, otherwise the finished item will be left in the Empowerer.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Open a Pattern Encoding Terminal, pick an empowering recipe in JEI and press **+**. The five ingredients land in the grid and the pattern encodes as an Empowering Pattern.
* Which stand gets which modifier does not matter. The Matrix and the Empowerer both work it out from the recipe.
* One pattern is one craft. Every slot the Empowerer uses holds a single item, so a pattern scaled up to two or more crafts is refused rather than half-staged.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **A stand is missing or occupied.** All four must be in place and empty before a craft can be staged. Anything left on a stand by hand blocks the push.
* **The stands are flat.** Each stand pays its share every tick, and a stand that runs dry pauses the craft with the items still in it. Actually Additions never gives those items back on its own, so keep the stands on a supply that can hold up.
* **The result never comes back.** Actually Additions only lets automation take an item out of an Empowerer when that item is not itself the base of another empowering recipe. No shipped recipe does that, but a datapack could, and such a result has to be taken out by hand.
* **The pattern is refused outright.** Re-encode it from JEI. A processing pattern that never matched an empowering recipe is remembered as unusable until the next `/reload`.
