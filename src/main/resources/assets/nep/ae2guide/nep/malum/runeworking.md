---
navigation:
  title: Runeworking
  icon: malum:runic_workbench
  parent: nep/malum/index.md
  position: 18
item_ids:
  - nep:runeworking_pattern
---
# <Color id="light_purple">Runeworking</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="light_purple">Runeworking</Color>

  <ItemImage id="malum:runic_workbench" scale="2"/>

  A Runic Workbench shapes runes from a stone or wood blank and a second ingredient. By hand the second one comes out of your fist, which is what makes runes the one Malum craft you cannot walk away from. A Pattern Provider supplies it from the network instead.
</Column>

<Recipe id="nep:module_status/runeworking"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="6">
  <Block id="malum:runic_workbench" y="0"/>
  <Block id="ae2:pattern_provider" y="1"/>

  <BlockAnnotation y="1" color="#00ccff">
    **Pattern Provider**, pushing patterns in
  </BlockAnnotation>
  <BlockAnnotation y="0" color="#cc66ff">
    **Runic Workbench**, shaping the rune itself
  </BlockAnnotation>
</GameScene>

* Put a Pattern Provider against any face of the workbench and drop <ItemLink id="nep:runeworking_pattern"/> patterns in it.
* Nothing goes on the bench by hand. The Provider lays the blank down and supplies the second ingredient at the same moment, which is the part a player would otherwise be holding.
* No Import Card is needed. The finished rune goes straight back to the Provider that ordered it, and only drops on the floor when nothing around the bench will take it.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">The Hand Is the Whole Problem</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

Every other Malum machine takes its ingredients from the world: an altar from its pedestals, a crucible from its own slot. A Runic Workbench takes the blank from the bench and the second ingredient from whatever you are holding when you click it, so a hopper or an export bus can fill the bench and still never produce a rune.

That second ingredient is usually spirits, and the counts are not small. Feeding it from the network is the difference between a rune line and a clicking chore.

> <Color id="yellow">The bench must be clear.</Color> A blank you left on it by hand blocks the Provider, because the pattern brings its own. Take it back off and the next push lands.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">In a Matrix Instead</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A <ItemLink id="nep:focused_spirit_matrix"/> runs runeworking patterns too, alongside its infusion and focusing work. The spirits a rune asks for come out of the Matrix's spirit bank like any other spirit cost, and Runewood Obelisks in its upgrade slot cut the time the same way they cut an infusion.

One Matrix therefore covers all three Malum systems, while a real workbench stays the cheaper option if runes are all you want.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pattern Encoding</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* In a Pattern Encoding Terminal, pick a runeworking recipe from JEI and hit the transfer arrow. The pattern encodes as a <ItemLink id="nep:runeworking_pattern"/>.
* A plain processing pattern works too, since every rune has its own output and the two ingredients name the recipe on their own.
* The pattern carries both ingredients as ordinary inputs. Which of the two ends up on the bench is decided by the recipe, not by the order you encoded them in.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

A push is refused, and tried again later, whenever the workbench cannot take a whole rune:

| Refusal | What to do |
|---|---|
| The workbench is already holding an item | Take it off; the pattern supplies its own blank |
| The workbench is still shaping a rune | Let the one it is running finish |
| The bench slot cannot hold the blank | The recipe asks for more than one slot takes, so run it in a Matrix |
