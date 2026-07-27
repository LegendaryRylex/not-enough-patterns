# Not Enough Patterns

Not Enough Patterns is an addon mod for Applied Energistics 2 that enhances its autocrafting functions by creating patterns for machines that pattern providers can't usually handle on their own.

Pattern providers only cope with machines shaped like a furnace: items in, items out. This mod teaches them the rest. Every integration is one ordinary autocrafting step: encode a pattern, drop it in a provider, and the network drives the machine.

## Supported Mods

| Mod | Processes |
|---|---|
| Create | Mechanical Crafting, Deploying, Filling, Sequenced Assembly |

Look a recipe up in JEI, hit the **+** button at a Pattern Encoding Terminal, and the right pattern comes back with the ingredients already worked out.

## Added Content

- **Import Card** — Allows a pattern provider to collect its own results, tracked per pattern and per side, so it never takes anything that it shouldn't.
- **Controllers** — Manages a whole multi-step or multi-block process under one single block, moving items to where they need to go, then back into your system once the craft is finished.
- **Matrices** — One-block late-game solutions that replace an entire multiblock process line, like Create's Sequenced Assembly.

## Documentation

Every machine has a full page in the **in-game AE2 Guide**, including setup diagrams, pattern encoding, troubleshooting, and its settings. Open the guide and look for *Not Enough Patterns*. That is the reference; this page is the summary.

## Configuration

NEP is built with pack devs in mind: `nep-server.toml` is fully modular. Every integration has its own section with an `enabled` toggle, and a master `modules.<mod>.allow_<mod>_module` as a master switch for that mod's integrations.