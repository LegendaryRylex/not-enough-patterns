package dev.rylex.nep;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NepConfig {

    private NepConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue DEBUG_LOGGING;
    private static final ModConfigSpec.IntValue IMPORT_CARD_GRACE;
    private static final ModConfigSpec.BooleanValue PROCESSING_PATTERN_CONVERSION;
    private static final ModConfigSpec.BooleanValue MACHINE_HUB;
    private static final ModConfigSpec.IntValue MACHINE_HUB_LINK_RANGE;
    private static final ModConfigSpec.IntValue MACHINE_HUB_MAXIMUM_LINKS;
    private static final ModConfigSpec.IntValue MACHINE_HUB_SCAN_BUDGET;
    private static final ModConfigSpec.IntValue MACHINE_HUB_CASING_DEPTH;
    private static final ModConfigSpec.BooleanValue ACTUALLY_ADDITIONS_OVERRIDE;
    private static final ModConfigSpec.BooleanValue ACTUALLY_ADDITIONS_EMPOWERING;
    private static final ModConfigSpec.BooleanValue ACTUALLY_ADDITIONS_ATOMIC_RECONSTRUCTION;
    private static final ModConfigSpec.BooleanValue ACTUALLY_ADDITIONS_MATRIX;
    private static final ModConfigSpec.BooleanValue ACTUALLY_ADDITIONS_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_ENERGY_COST;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_CHARGE_RATE;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_CAPACITY;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_EMPOWERING_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_RECONSTRUCTION_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.BooleanValue APOTHIC_OVERRIDE;
    private static final ModConfigSpec.BooleanValue APOTHIC_INFUSION;
    private static final ModConfigSpec.IntValue APOTHIC_INFUSION_EXPERIENCE_PER_BOTTLE;
    private static final ModConfigSpec.BooleanValue APOTHIC_INFUSION_PREFER_EXPERIENCE_FLUID;
    private static final ModConfigSpec.IntValue APOTHIC_INFUSION_MILLIBUCKETS_PER_EXPERIENCE;
    private static final ModConfigSpec.BooleanValue COMPACT_CRAFTING_OVERRIDE;
    private static final ModConfigSpec.BooleanValue COMPACT_CRAFTING_MATRIX;
    private static final ModConfigSpec.BooleanValue COMPACT_CRAFTING_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_MATRIX_CRAFT_TIME;
    private static final ModConfigSpec.ConfigValue<String> COMPACT_CRAFTING_MATRIX_MAXIMUM_FIELD_SIZE;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.BooleanValue COMPACT_CRAFTING_CONTROLLER;
    private static final ModConfigSpec.BooleanValue COMPACT_CRAFTING_CONTROLLER_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_CONTROLLER_BLOCKS_PER_TICK;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_CONTROLLER_ME_DRAIN;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_CONTROLLER_IDLE_ME_DRAIN;
    private static final ModConfigSpec.BooleanValue CREATE_OVERRIDE;
    private static final ModConfigSpec.BooleanValue CREATE_FILLING;
    private static final ModConfigSpec.BooleanValue CREATE_MECHANICAL_CRAFTING;
    private static final ModConfigSpec.BooleanValue CREATE_MECHANICAL_CRAFTING_ALLOW_REGULAR;
    private static final ModConfigSpec.BooleanValue CREATE_DEPLOYING;
    private static final ModConfigSpec.BooleanValue CREATE_DEPLOYING_LOG_STRIPPING;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_LINK_RANGE;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_TANK_CAPACITY;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_HALT_GRACE;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_RECLAIM_GRACE;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_MATRIX;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_GUARANTEED_RESULTS;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS_MINIMUM;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_MINIMUM_SPEED;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_TANK_CAPACITY;
    private static final ModConfigSpec.LongValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_ENERGY_CAPACITY;
    private static final ModConfigSpec.LongValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_CHARGE_RATE;
    private static final ModConfigSpec.BooleanValue DRACONIC_OVERRIDE;
    private static final ModConfigSpec.BooleanValue DRACONIC_FUSION_CRAFTING;
    private static final ModConfigSpec.BooleanValue DRACONIC_FUSION_MATRIX;
    private static final ModConfigSpec.BooleanValue DRACONIC_FUSION_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_ENERGY_COST;
    private static final ModConfigSpec.LongValue DRACONIC_FUSION_MATRIX_CHARGE_RATE;
    private static final ModConfigSpec.LongValue DRACONIC_FUSION_MATRIX_CAPACITY;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.ConfigValue<String> DRACONIC_FUSION_MATRIX_MAXIMUM_TIER;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_MAX_CORES;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_CORE_SWAP_GRACE;
    private static final ModConfigSpec.LongValue DRACONIC_FUSION_MATRIX_WYVERN_CAPACITY;
    private static final ModConfigSpec.LongValue DRACONIC_FUSION_MATRIX_DRACONIC_CAPACITY;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_DRACONIC_CRAFT_TIME;
    private static final ModConfigSpec.LongValue DRACONIC_FUSION_MATRIX_CHAOTIC_CAPACITY;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_CHAOTIC_ENERGY_COST;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_CHAOTIC_CRAFT_TICKS;
    private static final ModConfigSpec.BooleanValue MYSTICAL_OVERRIDE;
    private static final ModConfigSpec.BooleanValue MYSTICAL_INFUSION;
    private static final ModConfigSpec.BooleanValue MYSTICAL_AWAKENING;
    private static final ModConfigSpec.BooleanValue MYSTICAL_MATRIX;
    private static final ModConfigSpec.BooleanValue MYSTICAL_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue MYSTICAL_MATRIX_CRAFT_TICKS;
    private static final ModConfigSpec.LongValue MYSTICAL_MATRIX_TANK_CAPACITY;
    private static final ModConfigSpec.IntValue MYSTICAL_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue MYSTICAL_MATRIX_IDLE_ME_DRAIN;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                        "Per-mod integration modules with individual per-machine integration toggles.",
                        "Disabled integrations stop encoding patterns and stop pattern providers from pushing to those machines; already-encoded patterns stay in the world and resume working when the integration is re-enabled.")
                .translation("nep.configuration.modules")
                .push("modules");

        builder.comment("Actually Additions integration.")
                .translation("nep.configuration.modules.actuallyadditions")
                .push("actuallyadditions");
        ACTUALLY_ADDITIONS_OVERRIDE = builder.comment(
                        "Master override for Actually Additions integration.",
                        "When disabled, every Actually Additions module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.actuallyadditions.override")
                .define("allow_actuallyadditions_module", true);
        ACTUALLY_ADDITIONS_EMPOWERING = builder.comment(
                        "Empowering Module: An Empowerer with a pattern provider against it takes a whole recipe at once.",
                        "Each item in the craft goes to their designated recipe slot automatically. The base item goes to the Empowerer and the rest to each Display Stand.",
                        "The stands still supply the energy, and the finished item stays in the Empowerer.")
                .translation("nep.configuration.modules.actuallyadditions.empowering")
                .define("empowering", true);
        ACTUALLY_ADDITIONS_ATOMIC_RECONSTRUCTION = builder.comment(
                        "Atomic Reconstruction Module: Encodes Atomic Reconstructor recipes as patterns for the Atomic Empowering Matrix.",
                        "The patterns also push their ingredients into adjacent inventories like a plain processing pattern, so a dropper feeding a real Atomic Reconstructor works too.")
                .translation("nep.configuration.modules.actuallyadditions.atomicReconstruction")
                .define("atomicReconstruction", true);

        builder.comment("Atomic Empowering Matrix:")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix")
                .push("atomicEmpoweringMatrix");
        ACTUALLY_ADDITIONS_MATRIX = builder.comment(
                        "The Atomic Empowering Matrix runs whole empowering and atomic reconstruction recipes inside a single block.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.enabled")
                .define("enabled", true);
        ACTUALLY_ADDITIONS_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Matrix left short of materials stalls until you manually restock it.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.autoRequest")
                .define("autoRequest", true);
        ACTUALLY_ADDITIONS_MATRIX_ENERGY_COST = builder.comment(
                        "Energy a Matrix craft costs, as a percentage of the recipe's own energy cost.",
                        "100 charges exactly what the real machine would; raise it to make recipes more expensive.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.energyCost")
                .defineInRange("energyCostPercent", 100, 0, 10_000);
        ACTUALLY_ADDITIONS_MATRIX_CHARGE_RATE = builder.comment("Maximum energy the Matrix accepts per tick, in FE.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.chargeRate")
                .defineInRange("chargeRate", 40_000, 1, Integer.MAX_VALUE);
        ACTUALLY_ADDITIONS_MATRIX_CAPACITY = builder.comment("Size of the Matrix's internal energy buffer, in FE.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.capacity")
                .defineInRange("energyCapacity", 320_000, 1_000, Integer.MAX_VALUE);
        ACTUALLY_ADDITIONS_MATRIX_RECONSTRUCTION_CRAFT_TICKS = builder.comment(
                        "Ticks the Matrix spends on an atomic reconstruction craft. 20 ticks is one second.",
                        "The real Atomic Reconstructor fires once every 100 ticks and converts everything lying in its beam in that one pulse, so it has no per-item duration.")
                .translation(
                        "nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.atomicReconstructionCraftTicks")
                .defineInRange("atomicReconstructionCraftTicks", 4, 1, 24_000);
        ACTUALLY_ADDITIONS_MATRIX_EMPOWERING_CRAFT_TICKS = builder.comment(
                        "Ticks the Matrix spends on an empowering craft. 20 ticks is one second.",
                        "The recipe's own duration is not used: an empowering recipe takes between 30 and 500 ticks in the real multiblock.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.empoweringCraftTicks")
                .defineInRange("empoweringCraftTicks", 20, 1, 24_000);
        ACTUALLY_ADDITIONS_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while crafting, in AE per tick. Progress stops whenever the network cannot supply this much power.",
                        "This is separate from the FE it crafts with, and pays for the ME network side of the machine.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        ACTUALLY_ADDITIONS_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        builder.pop();

        builder.pop();

        builder.comment("Apothic Enchanting integration.")
                .translation("nep.configuration.modules.apothic_enchanting")
                .push("apothic_enchanting");
        APOTHIC_OVERRIDE = builder.comment(
                        "Master override for Apothic Enchanting integration.",
                        "When disabled, every Apothic Enchanting module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.apothic_enchanting.override")
                .define("allow_apothic_enchanting_module", true);

        builder.comment("Infusion Module:")
                .translation("nep.configuration.modules.apothic_enchanting.infusion")
                .push("infusion");
        APOTHIC_INFUSION = builder.comment(
                        "The Enchanting Table of the Raven runs infusion recipes pushed by a pattern provider, setting its eterna, quanta, and arcana to whatever the recipe needs and putting the player's own values back afterwards.")
                .translation("nep.configuration.modules.apothic_enchanting.infusion.enabled")
                .define("enabled", true);
        APOTHIC_INFUSION_EXPERIENCE_PER_BOTTLE = builder.comment(
                        "Experience points one Bottle o' Enchanting pays towards an automated infusion, which decides how many bottles a pattern asks for.",
                        "A thrown bottle is worth 3 to 11 points, so the default pays ten times what one is worth by hand and keeps the bottle count of a craft small. Lower values make automated infusion more expensive.")
                .translation("nep.configuration.modules.apothic_enchanting.infusion.experiencePerBottle")
                .defineInRange("experiencePerBottle", 70, 1, 1_000);
        APOTHIC_INFUSION_PREFER_EXPERIENCE_FLUID = builder.comment(
                        "Pays an automated infusion with a fluid from the #c:experience tag whenever one is loaded, falling back to Bottles o' Enchanting when the tag is empty.",
                        "When disabled, patterns always ask for bottles. Either payment is accepted by the table, so patterns already encoded keep working.")
                .translation("nep.configuration.modules.apothic_enchanting.infusion.preferExperienceFluid")
                .define("preferExperienceFluid", true);
        APOTHIC_INFUSION_MILLIBUCKETS_PER_EXPERIENCE = builder.comment(
                        "Millibuckets of experience fluid one experience point costs, which decides how much fluid a pattern asks for.")
                .translation("nep.configuration.modules.apothic_enchanting.infusion.millibucketsPerExperience")
                .defineInRange("millibucketsPerExperience", 20, 1, 1_000);
        builder.pop();

        builder.pop();

        builder.comment("Compact Crafting integration.")
                .translation("nep.configuration.modules.compactcrafting")
                .push("compactcrafting");
        COMPACT_CRAFTING_OVERRIDE = builder.comment(
                        "Master override for Compact Crafting integration.",
                        "When disabled, every Compact Crafting module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.compactcrafting.override")
                .define("allow_compactcrafting_module", true);

        builder.comment("Miniaturization Controller:")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationController")
                .push("miniaturizationController");
        COMPACT_CRAFTING_CONTROLLER = builder.comment(
                        "The Miniaturization Controller runs miniaturization patterns on a real projector field instead of inside itself: it stacks the recipe's blocks in the field, throws the catalyst in, and collects what the field produces.",
                        "It must be placed touching one of the field's projectors, which is how it reads that field's size and centre, and it carries no field size ceiling of its own.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationController.enabled")
                .define("enabled", true);
        COMPACT_CRAFTING_CONTROLLER_AUTO_REQUEST = builder.comment(
                        "Treats the Controller as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Controller left short of materials stalls until you restock it by hand.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationController.autoRequest")
                .define("autoRequest", true);
        COMPACT_CRAFTING_CONTROLLER_BLOCKS_PER_TICK = builder.comment(
                        "How many blocks the Controller places into the field per tick.",
                        "Raise it to fill the field faster, or past the block count of your largest recipe to have every layout appear in a single tick.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationController.blocksPerTick")
                .defineInRange("blocksPerTick", 1, 1, 4_096);
        COMPACT_CRAFTING_CONTROLLER_ME_DRAIN = builder.comment(
                        "Energy the Controller draws from its ME network while the field is crafting, in AE per tick. The field stops being paid for whenever the network cannot supply this much power.",
                        "The field itself keeps its own crafting time, so this is the whole running cost of the machine.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationController.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        COMPACT_CRAFTING_CONTROLLER_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Controller draws from its ME network while idle, in AE per tick. This standing cost is read once when the Controller joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationController.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        builder.pop();

        builder.comment("Miniaturization Matrix:")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationMatrix")
                .push("miniaturizationMatrix");
        COMPACT_CRAFTING_MATRIX = builder.comment(
                        "The Miniaturization Matrix runs miniaturization recipes inside a single block, without needing field projectors.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationMatrix.enabled")
                .define("enabled", true);
        COMPACT_CRAFTING_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Matrix left short of materials stalls until you restock it by hand.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationMatrix.autoRequest")
                .define("autoRequest", true);
        COMPACT_CRAFTING_MATRIX_CRAFT_TIME = builder.comment(
                        "Length of a Matrix craft, as a percentage of the recipe's own crafting time. A miniaturization recipe carries its own duration, 200 ticks unless the recipe says otherwise.",
                        "Raise it to make the single-block shortcut slower than building the field would be.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationMatrix.craftTimePercent")
                .defineInRange("craftTimePercent", 100, 1, 10_000);
        COMPACT_CRAFTING_MATRIX_MAXIMUM_FIELD_SIZE = builder.comment(
                        "Largest field a recipe may need for the Matrix to run it: small (3x3x3), medium (5x5x5), large (7x7x7), or absurd (9x9x9).",
                        "Lower it to keep the biggest recipes on the real projectors while the Matrix handles the smaller ones.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationMatrix.maximumFieldSize")
                .define("maximumFieldSize", "absurd");
        COMPACT_CRAFTING_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while crafting, in AE per tick. Progress stops whenever the network cannot supply this much power.",
                        "Miniaturization crafting has no energy cost of its own, so this is the whole running cost of the machine.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        COMPACT_CRAFTING_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        builder.pop();

        builder.pop();

        builder.comment("Create integration.")
                .translation("nep.configuration.modules.create")
                .push("create");
        CREATE_OVERRIDE = builder.comment(
                        "Master override for Create integration.",
                        "When disabled, every Create module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.create.override")
                .define("allow_create_module", true);
        CREATE_FILLING = builder.comment(
                        "Spout Filling Module: Automates `create:filling` recipes and Create's generic bucket and bottle filling.")
                .translation("nep.configuration.modules.create.filling")
                .define("filling", true);

        builder.comment("Deployer Module:")
                .translation("nep.configuration.modules.create.deploying")
                .push("deploying");
        CREATE_DEPLOYING = builder.comment(
                        "Automates the Deployer for create:deploying and create:item_application recipes.",
                        "The Depot is the machine: the base item is staged on it and a Deployer two blocks above presses onto it.")
                .translation("nep.configuration.modules.create.deploying.enabled")
                .define("enabled", true);
        CREATE_DEPLOYING_LOG_STRIPPING = builder.comment(
                        "Allows Deployers holding an axe strip a log on a Depot. Depends on the deploying module above.",
                        "Adds a real Deploying recipe for every log in #minecraft:logs that an axe can strip. The axe is not consumed and takes no damage, matching Create's own axe recipes for de-oxidising copper.")
                .translation("nep.configuration.modules.create.deploying.logStripping")
                .define("logStripping", true);
        builder.pop();

        builder.comment("Mechanical Crafter Module:")
                .translation("nep.configuration.modules.create.mechanicalCrafting")
                .push("mechanicalCrafting");
        CREATE_MECHANICAL_CRAFTING = builder.comment(
                        "Automates Create's Mechanical Crafters, with a pattern provider feeding the array.",
                        "The recipe is captured by building it once in a real crafter array rather than encoded at a terminal.")
                .translation("nep.configuration.modules.create.mechanicalCrafting.enabled")
                .define("enabled", true);
        CREATE_MECHANICAL_CRAFTING_ALLOW_REGULAR = builder.comment(
                        "Toggles crafting/processing patterns auto-resolving to vanilla shaped or shapeless recipes for Mechanical Crafters.",
                        "When disabled, only genuine Create Mechanical Crafting recipes are driven.",
                        "Also requires Create's own 'recipes.allowRegularCraftingInCrafter' to be enabled.")
                .translation("nep.configuration.modules.create.mechanicalCrafting.allowRegularCrafting")
                .define("allowRegularCrafting", true);
        builder.pop();

        builder.comment("Sequenced Assembly Controller:")
                .translation("nep.configuration.modules.create.sequencedAssembly")
                .push("sequencedAssembly");
        CREATE_SEQUENCED_ASSEMBLY = builder.comment(
                        "Automates `create:sequenced_assembly` on a real belt line built out of Create machines.",
                        "The Controller is a black box around the whole line: One pattern goes in and the finished item(s) comes out, with the Controller feeding every station and recirculating until it is done.")
                .translation("nep.configuration.modules.create.sequencedAssembly.enabled")
                .define("enabled", true);
        CREATE_SEQUENCED_ASSEMBLY_LINK_RANGE = builder.comment(
                        "Maximum distance, in blocks, that a linked input, output, or machine station may sit from the Sequenced Assembly Controller.",
                        "Endpoints beyond this range are rejected when syncing the linker, guarding against distant links that would force-load far chunks every poll.")
                .translation("nep.configuration.modules.create.sequencedAssembly.linkRange")
                .defineInRange("linkRange", 16, 1, 128);
        CREATE_SEQUENCED_ASSEMBLY_AUTO_REQUEST = builder.comment(
                        "Treats the Sequenced Assembly Controller as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a chance recipe rolls junk.",
                        "When disabled, an unlucky streak halts the controller until you restock it by hand.")
                .translation("nep.configuration.modules.create.sequencedAssembly.autoRequest")
                .define("autoRequest", true);
        CREATE_SEQUENCED_ASSEMBLY_TANK_CAPACITY = builder.comment(
                        "Capacity of the Sequenced Assembly Controller's staging tank, per distinct fluid, in millibuckets.",
                        "A pattern whose fluids will not fit is rejected until the Controller has fed what it already holds to its Spouts.")
                .translation("nep.configuration.modules.create.sequencedAssembly.tankCapacity")
                .defineInRange("tankCapacity", 64_000, 1_000, 1_000_000);
        CREATE_SEQUENCED_ASSEMBLY_HALT_GRACE = builder.comment(
                        "Ticks a recipe may make no progress before the Controller counts it as halted (out of materials).",
                        "When halted with another recipe waiting, the Controller cleans out its stations and rotates the stuck recipe to the back of its queue so the others keep running. 20 ticks is one second.")
                .translation("nep.configuration.modules.create.sequencedAssembly.haltGrace")
                .defineInRange("haltGrace", 100, 0, 24_000);
        CREATE_SEQUENCED_ASSEMBLY_RECLAIM_GRACE = builder.comment(
                        "Ticks the Controller keeps clearing its output Depot after its pending recipes are dropped or its buffers are emptied.",
                        "The clock restarts every time something is reclaimed. 20 ticks is one second.")
                .translation("nep.configuration.modules.create.sequencedAssembly.reclaimGrace")
                .defineInRange("reclaimGrace", 200, 0, 24_000);
        builder.pop();

        builder.comment("Sequenced Assembly Matrix:")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix")
                .push("sequencedAssemblyMatrix");
        CREATE_SEQUENCED_ASSEMBLY_MATRIX = builder.comment(
                        "The Sequenced Assembly Matrix runs a whole sequenced assembly recipe inside a single block.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.enabled")
                .define("enabled", true);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_GUARANTEED_RESULTS = builder.comment(
                        "Bypasses the output chances of sequenced assembly recipes, so the Matrix craft always yields the recipe's primary result.",
                        "When disabled, the Matrix rolls the recipe's result pool exactly as a physical line does: unlucky rolls produce junk, which is placed into network storage, and the ingredients for that attempt are lost.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.guaranteedResults")
                .define("guaranteedResults", true);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Sequenced Assembly Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "This lets a Matrix with guaranteedResults disabled retry after a chance recipe rolls junk. When disabled, an unlucky streak stalls the crafting network until manually fixed.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.autoRequest")
                .define("autoRequest", true);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while assembling, in AE per tick. Progress stops whenever the network cannot supply this much power.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS = builder.comment(
                        "Rotational stress the Matrix draws once it reaches Create's maximum rotation speed, in Create Stress Units.",
                        "Draw rises with shaft speed between minimumSpeed and that maximum. A Matrix drawing more stress assembles faster.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.stressUnits")
                .defineInRange("stressUnits", 294_912, 1, 16_777_216);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS_MINIMUM = builder.comment(
                        "Rotational stress the Matrix draws at minimumSpeed (see below), in Create Stress Units. This is the least stress a working Matrix can process recipes at.",
                        "The ratio between stressUnits and this figure is how fast a fully fed Matrix assembles.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.stressUnitsMinimum")
                .defineInRange("stressUnitsMinimum", 8_192, 1, 16_777_216);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_MINIMUM_SPEED = builder.comment(
                        "Shaft speed the Matrix needs to run, in RPM. Below this the Matrix does no work and draws no stress at all.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.minimumSpeed")
                .defineInRange("minimumSpeed", 32, 1, 1_024);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_CRAFT_TICKS = builder.comment(
                        "Ticks the Matrix takes to assemble one item while drawing its maximum stress.",
                        "At lower stress the craft takes proportionally longer, up to 'stressUnits / stressUnitsMinimum' times this value.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.craftTicks")
                .defineInRange("craftTicks", 5, 1, 1_200);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_TANK_CAPACITY = builder.comment(
                        "Capacity of each of the Matrix's four internal tanks, in millibuckets.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.tankCapacity")
                .defineInRange("tankCapacity", 64_000, 1_000, 1_000_000);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_ENERGY_CAPACITY = builder.comment(
                        "Size of the Matrix's internal energy buffer, in FE. Only sequenced assembly recipes with an energising step (Create: New Age) cost energy; the buffer grows on its own when a single craft needs more than this.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.energyCapacity")
                .defineInRange("energyCapacity", 1_000_000L, 1_000L, Integer.MAX_VALUE);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_CHARGE_RATE = builder.comment(
                        "Maximum energy the Matrix accepts per tick, in FE.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.chargeRate")
                .defineInRange("chargeRate", 100_000L, 1L, Integer.MAX_VALUE);
        builder.pop();

        builder.pop();

        builder.comment("Draconic Evolution integration.")
                .translation("nep.configuration.modules.draconicevolution")
                .push("draconicevolution");
        DRACONIC_OVERRIDE = builder.comment(
                        "Master override for Draconic Evolution integration.",
                        "When disabled, every Draconic Evolution module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.draconicevolution.override")
                .define("allow_draconicevolution_module", true);
        DRACONIC_FUSION_CRAFTING = builder.comment(
                        "Fusion Crafting Module: The Crafting Core is the machine, and the network stages the catalyst in the core, loads every Crafting Injector around it, and starts the craft.")
                .translation("nep.configuration.modules.draconicevolution.fusionCrafting")
                .define("fusionCrafting", true);

        builder.comment("Fusion Matrix:")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix")
                .push("fusionMatrix");
        DRACONIC_FUSION_MATRIX = builder.comment(
                        "The Fusion Matrix runs a whole fusion crafting recipe inside a single block, with no Crafting Core or Injectors.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.enabled")
                .define("enabled", true);
        DRACONIC_FUSION_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Fusion Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Matrix left short of materials stalls until you restock it by hand.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.autoRequest")
                .define("autoRequest", true);
        DRACONIC_FUSION_MATRIX_ENERGY_COST = builder.comment(
                        "Energy a Matrix craft costs, as a percentage of the fusion recipe's own energy cost.",
                        "100 charges exactly what the real multiblock would; raise it to make recipes more expensive.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.energyCost")
                .defineInRange("energyCostPercent", 100, 0, 10_000);
        DRACONIC_FUSION_MATRIX_CHARGE_RATE = builder.comment(
                        "Maximum energy the Matrix accepts per tick, in FE. Draconic Evolution's own OP transfer counts the same.",
                        "This is the speed limit on the charging phase, standing in for the charge rate of the Injectors it replaces.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.chargeRate")
                .defineInRange("chargeRate", 10_000_000L, 1L, Long.MAX_VALUE);
        DRACONIC_FUSION_MATRIX_CAPACITY = builder.comment(
                        "Size of an unupgraded Matrix's internal energy buffer, in FE.",
                        "The default is sized for the priciest Draconium tier recipe; upgrade cores raise it to their own tier's ceiling.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.capacity")
                .defineInRange("energyCapacity", 2_000_000L, 1_000L, Long.MAX_VALUE);
        DRACONIC_FUSION_MATRIX_CRAFT_TICKS = builder.comment(
                        "Ticks the Matrix spends on the crafting phase once it is fully charged. 20 ticks is one second.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.craftTicks")
                .defineInRange("craftTicks", 20, 1, 24_000);
        DRACONIC_FUSION_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while fusing, in AE per tick. Progress stops whenever the network cannot supply this much power.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        DRACONIC_FUSION_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        DRACONIC_FUSION_MATRIX_MAXIMUM_TIER = builder.comment(
                        "Highest fusion recipe tier the Matrix will run: 'draconium', 'wyvern', 'draconic', or 'chaotic'.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.maximumTier")
                .define("maximumTier", "chaotic");

        builder.comment(
                        "Draconic Evolution cores slotted into the Fusion Matrix's upgrade slot tune the machine to the tier of core it holds. Each higher tier compounds lower tiers' upgrades.",
                        "One slot means one tier at a time; Wyvern Cores increases energy buffer; Draconic Cores also increase fusion speed; Chaotic Cores also decrease energy costs.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades")
                .push("upgrades");
        DRACONIC_FUSION_MATRIX_MAX_CORES = builder.comment(
                        "Quantity of Cores accepted. Each is measured against a full slot, so lowering this makes each core better.",
                        "Set to 0 to disable the upgrade slot entirely.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades.maxCores")
                .defineInRange("maxCores", 16, 0, 64);
        DRACONIC_FUSION_MATRIX_CORE_SWAP_GRACE = builder.comment(
                        "Ticks the Matrix holds on to energy that no longer fits after cores leave the upgrade slot. The Matrix can be topped up or re-tiered without voiding the energy buffer. 20 ticks is one second.",
                        "The buffer reads as full for the window and takes no more energy in; anything still over the smaller buffer when the window closes is lost.",
                        "Set to 0 to void the excess the moment a core comes out.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades.coreSwapGrace")
                .defineInRange("coreSwapGrace", 100, 0, 24_000);
        DRACONIC_FUSION_MATRIX_WYVERN_CAPACITY = builder.comment(
                        "Energy buffer a full slot of Wyvern Cores reaches, in FE. Each core adds an equal share, so the buffer climbs in a straight line from energyCapacity to here.",
                        "The default covers the priciest Wyvern tier recipe, Awakened Draconium Block.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades.wyvernCapacity")
                .defineInRange("wyvernCapacity", 50_000_000L, 1_000L, Integer.MAX_VALUE);
        DRACONIC_FUSION_MATRIX_DRACONIC_CAPACITY = builder.comment(
                        "Energy buffer a full slot of Draconic Cores reaches, in FE. Each core multiplies rather than adds, so the buffer climbs steeply toward the end of the slot.",
                        "The default covers the priciest Draconic tier recipe, the Draconic Staff of Power.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades.draconicCapacity")
                .defineInRange("draconicCapacity", 256_000_000L, 1_000L, Integer.MAX_VALUE);
        DRACONIC_FUSION_MATRIX_DRACONIC_CRAFT_TIME = builder.comment(
                        "Crafting time fully upgraded Draconic Cores can cut, as a percentage. A partly filled slot scales linearly toward this cap.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades.draconicCraftTime")
                .defineInRange("draconicCraftTimeReductionPercent", 38, 0, 99);
        DRACONIC_FUSION_MATRIX_CHAOTIC_CAPACITY = builder.comment(
                        "Energy buffer a full slot of Chaotic Cores reaches, in FE, multiplying the same way Draconic Cores do.",
                        "The default covers the priciest recipe in the base mod, the Chaotic Staff of Power.",
                        "Keeping this under `maxInt` matters: Most cables will read the buffer as permanently full and stop pushing, though some mod's cables will still work.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades.chaoticCapacity")
                .defineInRange("chaoticCapacity", 1_024_000_000L, 1_000L, Integer.MAX_VALUE);
        DRACONIC_FUSION_MATRIX_CHAOTIC_ENERGY_COST = builder.comment(
                        "Most of a recipe's charged energy cost a full slot of Chaotic Cores can cut, as a percentage. A partly filled slot scales linearly toward this cap.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades.chaoticEnergyCost")
                .defineInRange("chaoticEnergyCostReductionPercent", 50, 0, 99);
        DRACONIC_FUSION_MATRIX_CHAOTIC_CRAFT_TICKS = builder.comment(
                        "Crafting phase length a full slot of Chaotic Cores reaches, in ticks. A partly filled slot scales linearly from craftTicks down to this floor.",
                        "Values above craftTicks are ignored, so Chaotic Cores never slow a Matrix down.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.upgrades.chaoticCraftTicks")
                .defineInRange("chaoticMinimumCraftTicks", 4, 1, 24_000);
        builder.pop();

        builder.pop();

        builder.pop();

        builder.comment("Mystical Agriculture integration.")
                .translation("nep.configuration.modules.mysticalagriculture")
                .push("mysticalagriculture");
        MYSTICAL_OVERRIDE = builder.comment(
                        "Master override for Mystical Agriculture integration.",
                        "When disabled, every Mystical Agriculture module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.mysticalagriculture.override")
                .define("allow_mysticalagriculture_module", true);
        MYSTICAL_INFUSION = builder.comment(
                        "Infusion Altar Module: An Infusion Altar with a pattern provider against it takes a whole craft at once, with the centre item going into the altar and one ingredient onto each of its Infusion Pedestals.",
                        "The altar starts itself but does NOT automatically return the finished product.")
                .translation("nep.configuration.modules.mysticalagriculture.infusion")
                .define("infusion", true);
        MYSTICAL_AWAKENING = builder.comment(
                        "Awakening Altar Module: An Awakening Altar with a pattern provider against it takes a whole craft at once, with the centre item going into the altar, one ingredient onto each of its four Awakening Pedestals, and each recipe's essences into the four Essence Vessels.",
                        "The altar starts itself but does NOT automatically return the finished product.")
                .translation("nep.configuration.modules.mysticalagriculture.awakening")
                .define("awakening", true);

        builder.comment("Infused Awakening Matrix:")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix")
                .push("infusedAwakeningMatrix");
        MYSTICAL_MATRIX = builder.comment(
                        "The Infused Awakening Matrix runs infusion and awakening recipes inside a single block.",
                        "Its four essence tanks stand in for the Essence Vessels an awakening recipe would otherwise need.")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix.enabled")
                .define("enabled", true);
        MYSTICAL_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Matrix left short of materials stalls until you restock it by hand.")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix.autoRequest")
                .define("autoRequest", true);
        MYSTICAL_MATRIX_CRAFT_TICKS = builder.comment(
                        "Ticks the Matrix spends on a craft, whichever recipe it is running. 20 ticks is one second.",
                        "Both altars normally take 100 ticks.")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix.craftTicks")
                .defineInRange("craftTicks", 20, 1, 24_000);
        MYSTICAL_MATRIX_TANK_CAPACITY = builder.comment(
                        "Capacity of the Infusion Matrix's essence tanks, per essence, in item quantity. A tank takes one essence at a time and is free for any other the moment it empties.",
                        "A regular Essence Vessel holds 40, which is exactly one craft of the priciest awakening recipe.")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix.tankCapacity")
                .defineInRange("essenceTankCapacity", 40L, 40L, 512L);
        MYSTICAL_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while crafting, in AE per tick. Progress stops whenever the network cannot supply this much power.",
                        "Infusion and awakening have no energy cost of their own, so this is the whole running cost of the machine.")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        MYSTICAL_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        builder.pop();

        builder.pop();

        builder.pop();

        builder.comment("Pattern behaviour outside any one integration.")
                .translation("nep.configuration.patterns")
                .push("patterns");
        PROCESSING_PATTERN_CONVERSION = builder.comment(
                        "Lets a NEP pattern be crafted back into a plain AE2 Processing Pattern carrying the same ingredients and result, one pattern per crafting grid.",
                        "This is the escape hatch for a pack that moves a recipe onto a machine NEP does not drive, so an encoded NEP pattern can never leave a player with no way to automate the craft.",
                        "Patterns that keep an ingredient instead of consuming it, such as Deploying and Filling, are refused: a Processing Pattern has no way to say an ingredient comes back, and converting one would consume the tool on every craft.")
                .translation("nep.configuration.patterns.processingPatternConversion")
                .define("processingPatternConversion", true);
        builder.pop();

        builder.comment("Machine Hub behaviour.")
                .translation("nep.configuration.machineHub")
                .push("machineHub");
        MACHINE_HUB = builder.comment(
                        "The Machine Hub presents the inventories it is linked to as one inventory, so a pattern provider against the Hub can load a multiblock whose item and fluid hatches are separate blocks.",
                        "When disabled, a placed Hub keeps its links but stops offering storage, so pattern providers ignore it entirely.")
                .translation("nep.configuration.machineHub.enabled")
                .define("enabled", true);
        MACHINE_HUB_LINK_RANGE = builder.comment(
                        "Maximum distance, in blocks, that a linked inventory may sit from the Machine Hub. This is also how far the Hub's scan may reach.",
                        "Links beyond this range are refused when syncing the Hub Linker, guarding against distant links that would force-load far chunks on every push.")
                .translation("nep.configuration.machineHub.linkRange")
                .defineInRange("linkRange", 16, 1, 64);
        MACHINE_HUB_MAXIMUM_LINKS = builder.comment(
                        "How many inventories one Machine Hub may be linked to, and how many a scan may propose. The screen shows twelve rows at a time and scrolls past that.")
                .translation("nep.configuration.machineHub.maximumLinks")
                .defineInRange("maximumLinks", 24, 1, 64);
        MACHINE_HUB_SCAN_BUDGET = builder.comment(
                        "How many blocks the Machine Hub's scan may walk through before it stops, which is what bounds the cost of pressing the button.",
                        "The scan spreads from the blocks touching the Hub through anything that looks like part of the same machine, so it never walks off into the terrain and rarely gets near this ceiling. Raise it for a machine larger than the budget can cover.")
                .translation("nep.configuration.machineHub.scanBudget")
                .defineInRange("scanBudget", 16, 4, 128);
        MACHINE_HUB_CASING_DEPTH = builder.comment(
                        "How many plain blocks in a row the Machine Hub's scan may step through between one machine part and the next.",
                        "The scan travels freely through blocks carrying a block entity, which is every hatch, port and controller. Everything else is casing, and casing is only crossed for this many blocks at a time, and only when it belongs to a mod that already owns a machine part nearby. Raise it for a multiblock with thick walls between its hatches; set it to zero to have the scan follow machine parts alone.")
                .translation("nep.configuration.machineHub.casingDepth")
                .defineInRange("casingDepth", 3, 0, 8);
        builder.pop();

        builder.comment("Pattern Provider behaviour.")
                .translation("nep.configuration.provider")
                .push("provider");
        IMPORT_CARD_GRACE = builder.comment(
                        "Ticks an Import Card keeps collecting a result the network has stopped waiting for. This is what returns items already sent to a machine when a job is cancelled partway through.",
                        "The clock restarts every time the card collects something, so a run of results still in flight all come back. This grace period only runs once a job ends.",
                        "Set to 0 to give a cancelled job a single collection attempt and nothing more.")
                .translation("nep.configuration.provider.importCardGrace")
                .defineInRange("importCardGrace", 200, 0, 24_000);
        builder.pop();

        builder.comment("Mod Debug configuration.")
                .translation("nep.configuration.debug")
                .push("debug");
        DEBUG_LOGGING = builder.comment(
                        "Log diagnostics to the server log at INFO level (e.g. every pattern-provider push to a machine and why it was accepted or rejected).")
                .translation("nep.configuration.debug.logging")
                .define("verbose_logging", false);
        builder.pop();

        SPEC = builder.build();
    }

    public static void onConfigLoaded(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        int idleDrain = CREATE_SEQUENCED_ASSEMBLY_MATRIX_IDLE_ME_DRAIN.get();
        int activeDrain = CREATE_SEQUENCED_ASSEMBLY_MATRIX_ME_DRAIN.get();
        if (idleDrain > activeDrain) {
            Nep.LOGGER.warn(
                    "Sequenced Assembly Matrix idleMeNetworkDrain ({} AE/t) is above meNetworkDrain ({} AE/t); the idle"
                            + " drain is clamped to {} AE/t, so assembling costs no more than sitting idle.",
                    idleDrain,
                    activeDrain,
                    activeDrain);
        }
        int minimumStress = CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS_MINIMUM.get();
        int maximumStress = CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS.get();
        if (minimumStress > maximumStress) {
            Nep.LOGGER.warn(
                    "Sequenced Assembly Matrix stressUnitsMinimum ({} SU) is above stressUnits ({} SU); the floor is"
                            + " clamped to {} SU, which flattens the speed ramp so shaft speed no longer changes how"
                            + " fast the Matrix assembles.",
                    minimumStress,
                    maximumStress,
                    maximumStress);
        }
    }

    public static boolean loaded() {
        return SPEC.isLoaded();
    }

    public static int importCardGrace() {
        return SPEC.isLoaded() ? IMPORT_CARD_GRACE.get() : 200;
    }

    public static boolean debugLogging() {
        return SPEC.isLoaded() && DEBUG_LOGGING.get();
    }

    public static boolean processingPatternConversion() {
        return SPEC.isLoaded() && PROCESSING_PATTERN_CONVERSION.get();
    }

    public static boolean machineHubEnabled() {
        return !SPEC.isLoaded() || MACHINE_HUB.get();
    }

    public static int machineHubLinkRange() {
        return SPEC.isLoaded() ? MACHINE_HUB_LINK_RANGE.get() : 16;
    }

    public static int machineHubMaximumLinks() {
        return SPEC.isLoaded() ? MACHINE_HUB_MAXIMUM_LINKS.get() : 24;
    }

    public static int machineHubScanBudget() {
        return SPEC.isLoaded() ? MACHINE_HUB_SCAN_BUDGET.get() : 16;
    }

    public static int machineHubCasingDepth() {
        return SPEC.isLoaded() ? MACHINE_HUB_CASING_DEPTH.get() : 3;
    }

    public static boolean actuallyAdditionsOverride() {
        return SPEC.isLoaded() && ACTUALLY_ADDITIONS_OVERRIDE.get();
    }

    public static boolean actuallyAdditionsEmpowering() {
        return actuallyAdditionsOverride() && ACTUALLY_ADDITIONS_EMPOWERING.get();
    }

    public static boolean actuallyAdditionsAtomicReconstruction() {
        return actuallyAdditionsOverride() && ACTUALLY_ADDITIONS_ATOMIC_RECONSTRUCTION.get();
    }

    public static boolean actuallyAdditionsMatrix() {
        return actuallyAdditionsOverride() && ACTUALLY_ADDITIONS_MATRIX.get();
    }

    public static boolean actuallyAdditionsMatrixAutoRequest() {
        return actuallyAdditionsMatrix() && ACTUALLY_ADDITIONS_MATRIX_AUTO_REQUEST.get();
    }

    public static int actuallyAdditionsMatrixEnergyCostPercent() {
        return SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_ENERGY_COST.get() : 100;
    }

    public static int actuallyAdditionsMatrixChargeRate() {
        return SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_CHARGE_RATE.get() : 40_000;
    }

    public static int actuallyAdditionsMatrixCapacity() {
        return SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_CAPACITY.get() : 320_000;
    }

    public static int actuallyAdditionsMatrixEmpoweringCraftTicks() {
        return SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_EMPOWERING_CRAFT_TICKS.get() : 20;
    }

    public static int actuallyAdditionsMatrixAtomicReconstructionCraftTicks() {
        return SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_RECONSTRUCTION_CRAFT_TICKS.get() : 4;
    }

    public static int actuallyAdditionsMatrixMeDrain() {
        return SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int actuallyAdditionsMatrixIdleMeDrain() {
        int idle = SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, actuallyAdditionsMatrixMeDrain());
    }

    public static boolean apothicOverride() {
        return SPEC.isLoaded() && APOTHIC_OVERRIDE.get();
    }

    public static boolean apothicInfusion() {
        return apothicOverride() && APOTHIC_INFUSION.get();
    }

    public static int apothicInfusionExperiencePerBottle() {
        return SPEC.isLoaded() ? APOTHIC_INFUSION_EXPERIENCE_PER_BOTTLE.get() : 70;
    }

    public static boolean apothicInfusionPreferExperienceFluid() {
        return !SPEC.isLoaded() || APOTHIC_INFUSION_PREFER_EXPERIENCE_FLUID.get();
    }

    public static int apothicInfusionMillibucketsPerExperience() {
        return SPEC.isLoaded() ? APOTHIC_INFUSION_MILLIBUCKETS_PER_EXPERIENCE.get() : 20;
    }

    public static boolean compactCraftingOverride() {
        return SPEC.isLoaded() && COMPACT_CRAFTING_OVERRIDE.get();
    }

    public static boolean compactCraftingMiniaturizationMatrix() {
        return compactCraftingOverride() && COMPACT_CRAFTING_MATRIX.get();
    }

    public static boolean compactCraftingMatrixAutoRequest() {
        return compactCraftingMiniaturizationMatrix() && COMPACT_CRAFTING_MATRIX_AUTO_REQUEST.get();
    }

    public static int compactCraftingMatrixCraftTimePercent() {
        return SPEC.isLoaded() ? COMPACT_CRAFTING_MATRIX_CRAFT_TIME.get() : 100;
    }

    public static String compactCraftingMatrixMaximumFieldSize() {
        return SPEC.isLoaded() ? COMPACT_CRAFTING_MATRIX_MAXIMUM_FIELD_SIZE.get() : "absurd";
    }

    public static int compactCraftingMatrixMeDrain() {
        return SPEC.isLoaded() ? COMPACT_CRAFTING_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int compactCraftingMatrixIdleMeDrain() {
        int idle = SPEC.isLoaded() ? COMPACT_CRAFTING_MATRIX_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, compactCraftingMatrixMeDrain());
    }

    public static boolean compactCraftingMiniaturizationController() {
        return compactCraftingOverride() && COMPACT_CRAFTING_CONTROLLER.get();
    }

    public static boolean compactCraftingControllerAutoRequest() {
        return compactCraftingMiniaturizationController() && COMPACT_CRAFTING_CONTROLLER_AUTO_REQUEST.get();
    }

    public static int compactCraftingControllerBlocksPerTick() {
        return SPEC.isLoaded() ? COMPACT_CRAFTING_CONTROLLER_BLOCKS_PER_TICK.get() : 1;
    }

    public static int compactCraftingControllerMeDrain() {
        return SPEC.isLoaded() ? COMPACT_CRAFTING_CONTROLLER_ME_DRAIN.get() : 500;
    }

    public static int compactCraftingControllerIdleMeDrain() {
        int idle = SPEC.isLoaded() ? COMPACT_CRAFTING_CONTROLLER_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, compactCraftingControllerMeDrain());
    }

    public static boolean createOverride() {
        return SPEC.isLoaded() && CREATE_OVERRIDE.get();
    }

    public static boolean createMechanicalCrafting() {
        return createOverride() && CREATE_MECHANICAL_CRAFTING.get();
    }

    public static boolean createMechanicalCraftingAllowRegular() {
        return SPEC.isLoaded() && CREATE_MECHANICAL_CRAFTING_ALLOW_REGULAR.get();
    }

    public static boolean createDeploying() {
        return createOverride() && CREATE_DEPLOYING.get();
    }

    public static boolean createDeployingLogStripping() {
        return createDeploying() && CREATE_DEPLOYING_LOG_STRIPPING.get();
    }

    public static boolean createFilling() {
        return createOverride() && CREATE_FILLING.get();
    }

    public static boolean createSequencedAssembly() {
        return createOverride() && CREATE_SEQUENCED_ASSEMBLY.get();
    }

    public static int createSequencedAssemblyLinkRange() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_LINK_RANGE.get() : 16;
    }

    public static boolean createSequencedAssemblyAutoRequest() {
        return createSequencedAssembly() && CREATE_SEQUENCED_ASSEMBLY_AUTO_REQUEST.get();
    }

    public static int createSequencedAssemblyTankCapacity() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_TANK_CAPACITY.get() : 64_000;
    }

    public static int createSequencedAssemblyHaltGrace() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_HALT_GRACE.get() : 100;
    }

    public static int createSequencedAssemblyReclaimGrace() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_RECLAIM_GRACE.get() : 200;
    }

    public static boolean createSequencedAssemblyMatrix() {
        return createOverride() && CREATE_SEQUENCED_ASSEMBLY_MATRIX.get();
    }

    public static boolean createSequencedAssemblyMatrixGuaranteedResults() {
        return !SPEC.isLoaded() || CREATE_SEQUENCED_ASSEMBLY_MATRIX_GUARANTEED_RESULTS.get();
    }

    public static boolean createSequencedAssemblyMatrixAutoRequest() {
        return createSequencedAssemblyMatrix() && CREATE_SEQUENCED_ASSEMBLY_MATRIX_AUTO_REQUEST.get();
    }

    public static int createSequencedAssemblyMatrixMeDrain() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int createSequencedAssemblyMatrixStress() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS.get() : 294_912;
    }

    public static int createSequencedAssemblyMatrixIdleMeDrain() {
        int idle = SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, createSequencedAssemblyMatrixMeDrain());
    }

    public static int createSequencedAssemblyMatrixStressMinimum() {
        int minimum = SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS_MINIMUM.get() : 8_192;
        return Math.min(minimum, createSequencedAssemblyMatrixStress());
    }

    public static int createSequencedAssemblyMatrixMinimumSpeed() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_MINIMUM_SPEED.get() : 32;
    }

    public static int createSequencedAssemblyMatrixCraftTicks() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_CRAFT_TICKS.get() : 5;
    }

    public static int createSequencedAssemblyMatrixTankCapacity() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_TANK_CAPACITY.get() : 64_000;
    }

    public static long createSequencedAssemblyMatrixEnergyCapacity() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_ENERGY_CAPACITY.get() : 1_000_000L;
    }

    public static long createSequencedAssemblyMatrixChargeRate() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_CHARGE_RATE.get() : 100_000L;
    }

    public static boolean draconicOverride() {
        return SPEC.isLoaded() && DRACONIC_OVERRIDE.get();
    }

    public static boolean draconicFusionCrafting() {
        return draconicOverride() && DRACONIC_FUSION_CRAFTING.get();
    }

    public static boolean draconicFusionMatrix() {
        return draconicOverride() && DRACONIC_FUSION_MATRIX.get();
    }

    public static boolean draconicFusionMatrixAutoRequest() {
        return draconicFusionMatrix() && DRACONIC_FUSION_MATRIX_AUTO_REQUEST.get();
    }

    public static int draconicFusionMatrixEnergyCostPercent() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_ENERGY_COST.get() : 100;
    }

    public static long draconicFusionMatrixChargeRate() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_CHARGE_RATE.get() : 10_000_000L;
    }

    public static long draconicFusionMatrixCapacity() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_CAPACITY.get() : 2_000_000L;
    }

    public static int draconicFusionMatrixCraftTicks() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_CRAFT_TICKS.get() : 20;
    }

    public static int draconicFusionMatrixMeDrain() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int draconicFusionMatrixIdleMeDrain() {
        int idle = SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, draconicFusionMatrixMeDrain());
    }

    public static int draconicFusionMatrixMaxCores() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_MAX_CORES.get() : 16;
    }

    public static int draconicFusionMatrixCoreSwapGrace() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_CORE_SWAP_GRACE.get() : 100;
    }

    public static long draconicFusionMatrixWyvernCapacity() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_WYVERN_CAPACITY.get() : 50_000_000L;
    }

    public static long draconicFusionMatrixDraconicCapacity() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_DRACONIC_CAPACITY.get() : 256_000_000L;
    }

    public static int draconicFusionMatrixDraconicCraftTimeReductionPercent() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_DRACONIC_CRAFT_TIME.get() : 38;
    }

    public static long draconicFusionMatrixChaoticCapacity() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_CHAOTIC_CAPACITY.get() : 1_024_000_000L;
    }

    public static int draconicFusionMatrixChaoticEnergyCostReductionPercent() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_CHAOTIC_ENERGY_COST.get() : 50;
    }

    public static int draconicFusionMatrixChaoticMinimumCraftTicks() {
        int floor = SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_CHAOTIC_CRAFT_TICKS.get() : 4;
        return Math.min(floor, draconicFusionMatrixCraftTicks());
    }

    public static String draconicFusionMatrixMaximumTier() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_MAXIMUM_TIER.get() : "chaotic";
    }

    public static boolean mysticalOverride() {
        return SPEC.isLoaded() && MYSTICAL_OVERRIDE.get();
    }

    public static boolean mysticalInfusion() {
        return mysticalOverride() && MYSTICAL_INFUSION.get();
    }

    public static boolean mysticalAwakening() {
        return mysticalOverride() && MYSTICAL_AWAKENING.get();
    }

    public static boolean mysticalInfusedAwakeningMatrix() {
        return mysticalOverride() && MYSTICAL_MATRIX.get();
    }

    public static boolean mysticalInfusedAwakeningMatrixAutoRequest() {
        return mysticalInfusedAwakeningMatrix() && MYSTICAL_MATRIX_AUTO_REQUEST.get();
    }

    public static int mysticalInfusedAwakeningMatrixCraftTicks() {
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_CRAFT_TICKS.get() : 100;
    }

    public static long mysticalInfusedAwakeningMatrixTankCapacity() {
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_TANK_CAPACITY.get() : 40L;
    }

    public static int mysticalInfusedAwakeningMatrixMeDrain() {
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int mysticalInfusedAwakeningMatrixIdleMeDrain() {
        int idle = SPEC.isLoaded() ? MYSTICAL_MATRIX_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, mysticalInfusedAwakeningMatrixMeDrain());
    }
}
