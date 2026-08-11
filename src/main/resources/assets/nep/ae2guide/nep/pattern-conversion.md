---
navigation:
  title: Pattern Conversion
  icon: ae2:processing_pattern
  parent: nep/nep-index.md
  position: 3
---
# <Color id="aqua">Pattern Conversion</Color>

<Column alignItems="center" fullWidth={true}>
  # <Color id="aqua">Pattern Conversion</Color>

  <ItemImage id="ae2:processing_pattern" scale="2"/>

  Any NEP pattern can be crafted back into a plain AE2 Processing Pattern carrying the same ingredients and the same result. Drop one pattern into a crafting grid on its own and the Processing Pattern comes back out.
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">When You Need It</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

NEP claims the <Color id="green">+</Color> button for every process it supports, so looking a recipe up and encoding it always hands back a NEP pattern. That is the right answer almost every time, but not always:

* A pack has moved the recipe onto a machine of its own, and that machine wants ordinary items in and items out.
* You are feeding the craft to something else entirely, like a packager or another mod's autocrafting block.
* The integration is switched off in the config and you still want the recipe automated.

In each of those cases the NEP pattern is the wrong shape and there is no other way to get the plain one. Converting it hands you an ordinary Processing Pattern that any Pattern Provider will push into any inventory.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What Comes Back</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

The Processing Pattern carries the ingredients the NEP pattern was holding, added together per item, and the result it was producing. Grid shape and ordering are dropped, because a Processing Pattern has nowhere to keep them.

It does quietly remember which recipe it came from, which is how you get the NEP pattern back: drop the Processing Pattern into the pattern slot of a Pattern Encoding Terminal and press <Color id="green">Encode</Color>. The terminal knows the recipe again, so it hands back the NEP pattern rather than another Processing Pattern, and you never have to look the recipe up a second time. Mechanical Crafting patterns are the one exception, since they are encoded from a grid rather than from a recipe; those are matched by their result instead, which works whenever a single recipe makes that result.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">What Cannot Convert</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

<Color id="red">A pattern that keeps one of its ingredients will not convert.</Color> Deploying and Filling patterns hold a tool that the machine gives back once the craft is done, and a Processing Pattern has no way to say so. Converting one would ask your network for a fresh tool on every single craft, so the recipe simply does not match and nothing appears in the output slot.

Everything else converts, including Mechanical Crafting patterns and every Matrix pattern.

<ItemImage id="minecraft:air" scale="0.25"/>

***

<Column alignItems="center" fullWidth={true}>
  ## <Color id="gold">Configs for Pack Devs</Color>
</Column>

<ItemImage id="minecraft:air" scale="0.25"/>

`Patterns → Convert Patterns To Processing Patterns` turns the recipe off. Leave it on unless your pack depends on a process being reachable only through its NEP pattern, since it is what keeps a player from being stranded when a recipe has been moved somewhere NEP does not drive.
