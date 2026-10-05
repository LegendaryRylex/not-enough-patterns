---
navigation:
  title: Pure and Radiant Spirits
  icon: nep:radiant_spirit
  parent: nep/malum/index.md
  position: 25
item_ids:
  - nep:pure_spirit
  - nep:radiant_spirit
---
# <Color id="light_purple">Pure and Radiant Spirits</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="light_purple">Pure and Radiant Spirits</Color>

  <ItemImage id="nep:radiant_spirit" scale="2"/>

  Two spirit types beyond Malum's eight, sitting past the point where the other eight stop being scarce. They behave like any other spirit: they stack in a Spirit Jar, feed a Spirit Altar or a Focused Spirit Matrix, and a pattern can ask the network for them.
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Pure Spirit</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<Column alignItems="center" fullWidth={true}>
  <ItemImage id="nep:pure_spirit" scale="2"/>
</Column>

A Pure Spirit is what is left when something dies with nothing owed. It cannot be infused or focused into being, only reaped: harvest the soul of a boss and one comes loose about one time in twenty. The entities that can give one up are the `nep:pure_spirit_sources` entity tag, and the chance is the **Pure Spirit Drop Chance** setting in the Malum config section.

Being untouched is also its weakness. Drop one down a Weeping Well and the void takes it whole, handing back an Umbral Spirit.

<Recipe id="nep:pure_spirit_to_umbral"/>

<ItemImage id="minecraft:air" scale="0.5"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Radiant Spirit</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<Column alignItems="center" fullWidth={true}>
  <ItemImage id="nep:radiant_spirit" scale="2"/>
</Column>

A Radiant Spirit holds all eight arcana at once without any of them winning, which is why its colour never settles. Binding one takes a Stellar Mechanism, an Impurity Stabilizer to keep the mixture from collapsing, four Paracausal Flames, a heavy pour of all eight spirits, and the Pure Spirits that give it something to bind to.

<Recipe id="nep:radiant_spirit"/>

<ItemImage id="minecraft:air" scale="0.25"/>

It is the last ingredient in a Matrix Catalyzer, which is where a Focused Spirit Matrix picks up chain focusing and its fortune roll.

<Recipe id="nep:matrix_catalyzer"/>

<ItemImage id="minecraft:air" scale="0.5"/>
