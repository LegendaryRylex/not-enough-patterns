---
navigation:
  title: Spirit Provider
  icon: nep:spirit_provider
  parent: nep/occultism/index.md
  position: 30
item_ids:
  - nep:spirit_provider
---
# <Color id="gold">Spirit Provider</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="gold">Spirit Provider</Color>

  <ItemImage id="nep:spirit_provider" scale="2"/>

  A bowl that hands a Pattern Provider's ingredients to your own working spirits, and catches what they make before it ever reaches the ground.
</Column>

<Recipe id="nep:module_status/spirit_provider"/>

<Recipe id="nep:spirit_provider"/>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Setup</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Place the Provider where a crusher, crystallizer, smelter or trader spirit's work area covers it, and put a Pattern Provider against it.
* Fill the Pattern Provider with plain **Processing Patterns** for that spirit's recipes.
* The Provider only serves a spirit whose work area covers it, and only takes a pattern when a spirit nearby can work every one of its inputs. Anything else is turned away for the network to send elsewhere.
* Finished products go back into the Pattern Provider on their own, so it needs no Import Card.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">How It Works</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* The Provider sets out one stack at a time in its bowl. A spirit picks it up just as it would any item thrown on the ground, and works it at its own speed and with its own output.
* The result is taken back the moment the spirit finishes, rather than dropped.
* A stack nobody collects within 30 seconds is taken back and set out afresh.
* It holds up to <nep:ConfigValue name="spiritProviderQueueLimit"/> crafts at once. While full it turns patterns away, and the network tries again later.

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Keep In Mind</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

* Hoppers leave a stack in the bowl alone. Another mod's item collector in reach can still grab it before the spirit does, and that craft then never finishes.
* The Provider recognises a result by the item that went in, so two crafts from the same input at once share their results.
