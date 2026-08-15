---
navigation:
  title: Miniaturization Controller
  icon: nep:miniaturization_controller
  parent: nep/compactcrafting/index.md
  position: 51
item_ids:
  - nep:miniaturization_controller
---
# <Color id="gold">Miniaturization Controller</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Miniaturization Controller</Color>

  <ItemImage id="nep:miniaturization_controller" scale="2"/>

  The other half of miniaturization automation. Where the Matrix replaces the field, the Controller drives it: it stacks the recipe's blocks between your projectors, throws the catalyst in, and collects what the field spits out. There is no field size ceiling, because the field is real.
</Column>

<Recipe id="nep:module_status/miniaturization_controller"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<GameScene zoom="5">
  <Block id="compactcrafting:field_projector" y="1"/>
  <Block id="nep:miniaturization_controller" z="0"/>
  <Block id="ae2:pattern_provider" z="1"/>
</GameScene>

* Build the projector field as you normally would, then place the Controller so it **touches one of the four projectors** on any face. That contact is the whole binding: the projector's own block state carries the field size and facing, which is enough to name the field's centre.
* Put a Pattern Provider against any other face and drop <ItemLink id="nep:miniaturization_pattern"/> patterns in it. The Controller buffers the blocks and the catalyst as they arrive.
* The Controller is an ME machine: it joins the network through its own grid connection, draws a small standing power cost, and returns finished items to the Provider on its own. It needs **no Import Card**.
* It refuses patterns while it has no field to drive, so jobs are never routed into a Controller that cannot run them.

> <Color id="red">Never wire redstone into the Controller.</Color> Any signal reaching a projector switches the whole field off, cancelling a craft in progress. The Controller itself neither emits nor conducts power, so it is safe on its own; a repeater or comparator pointed at the projector is not.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How It Crafts</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.35"/>

1) The Controller places the recipe's blocks inside the field a block a tick, stacked from the bottom up and anchored the way a player would build them, so a large recipe visibly rises between the projectors.

2) Once the field reports a match on the recipe the pattern named, the catalyst is dropped in as a loose item, exactly as if you had thrown it. The field consumes one and the blocks vanish.

3) The field then runs the recipe's own crafting time. The bar in the controller's screen tracks the field's progress. 

4) Once the recipe is finished the Controller pulls the crafted item back in. Anything left of the catalyst stack goes back to the input buffer.

If the field refuses the layout, the blocks are taken back out and the ingredients return to the Controller buffer, so a mismatch costs nothing.

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Screen</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The top readout names the current stage and the size of the field it is bound to, or **No field** when it is not touching a live projector.
* The input buffer only accepts items a pending craft still needs, so loose items cannot clog it.
* **Clear Pending Recipes** drops every craft the Controller still owes, cancels ingredients it has on request, and cancels the crafting jobs on the network that were waiting on it.
* **Empty Buffers** returns everything staged to your inventory.
* **Guide** opens this page, and is always the rightmost button.
* A Comparator beside the block reads the output buffer, empty for nothing staged up to a full 15.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">If Nothing Happens</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* **No projector field.** The Controller has to touch one of the four projectors, and the field has to still be formed. A projector goes dark when its opposite number is missing.
* **Blocks left in the field.** The Controller only builds into an empty field. Clear out anything standing inside it, including blocks a cancelled craft left behind.
* **Field switched off** or **Field already crafting.** A redstone signal on any projector disables the field; a match the field found on its own has to finish first.
* **Field too small.** Move the projectors further apart until the field is large enough for the recipe.
* The Controller must be on a powered ME network, not just connected to one.
* The Controller takes up 7 channels by default, which is nearly a whole normal cable's worth. A network that cannot spare them leaves it offline.
* Turn on Verbose Logging under Debug to have the Controller log every layout it builds, abandons, or loses.