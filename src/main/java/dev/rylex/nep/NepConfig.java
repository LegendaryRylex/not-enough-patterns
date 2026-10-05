package dev.rylex.nep;

import dev.rylex.nep.decoder.DecoderModule;
import dev.rylex.nep.hub.HubRules;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NepConfig {

    private NepConfig() {}

    public static final ModConfigSpec SPEC;

    public static final int CHAIN_FOCUSING_REMAINDER_TICKS = 10;

    private static final ModConfigSpec.BooleanValue DEBUG_LOGGING;
    private static final ModConfigSpec.IntValue IMPORT_CARD_GRACE;
    private static final ModConfigSpec.BooleanValue PROCESSING_PATTERN_CONVERSION;
    private static final ModConfigSpec.BooleanValue MANUAL_CRAFTING;
    private static final ModConfigSpec.BooleanValue MACHINE_HUB;
    private static final ModConfigSpec.IntValue MACHINE_HUB_LINK_RANGE;
    private static final ModConfigSpec.IntValue MACHINE_HUB_MAXIMUM_LINKS;
    private static final ModConfigSpec.IntValue MACHINE_HUB_SCAN_BUDGET;
    private static final ModConfigSpec.IntValue MACHINE_HUB_CASING_DEPTH;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MACHINE_HUB_SCAN_WHITELIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MACHINE_HUB_SCAN_BLACKLIST;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> MACHINE_HUB_LINK_BLACKLIST;
    private static final ModConfigSpec.IntValue MACHINE_HUB_CHANNELS;
    private static final ModConfigSpec.IntValue MACHINE_HUB_CHANNELS_PER_LINK;
    private static final ModConfigSpec.BooleanValue REQUIRE_DECODER;
    private static final ModConfigSpec.IntValue PATTERN_DECODER_CHANNELS;
    private static final ModConfigSpec.IntValue PATTERN_DECODER_IDLE_ME_DRAIN;
    private static final Map<DecoderModule, ModConfigSpec.BooleanValue> DECODER_MODULES =
            new EnumMap<>(DecoderModule.class);
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
    private static final ModConfigSpec.BooleanValue ACTUALLY_ADDITIONS_MATRIX_ME_CHARGE;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue ACTUALLY_ADDITIONS_MATRIX_CHANNELS;
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
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_MATRIX_CHANNELS;
    private static final ModConfigSpec.BooleanValue COMPACT_CRAFTING_CONTROLLER;
    private static final ModConfigSpec.BooleanValue COMPACT_CRAFTING_CONTROLLER_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_CONTROLLER_BLOCKS_PER_TICK;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_CONTROLLER_ME_DRAIN;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_CONTROLLER_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue COMPACT_CRAFTING_CONTROLLER_CHANNELS;
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
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_CHANNELS;
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
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_ME_CHARGE;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_CHANNELS;
    private static final ModConfigSpec.BooleanValue DRACONIC_OVERRIDE;
    private static final ModConfigSpec.BooleanValue DRACONIC_FUSION_CRAFTING;
    private static final ModConfigSpec.BooleanValue DRACONIC_FUSION_MATRIX;
    private static final ModConfigSpec.BooleanValue DRACONIC_FUSION_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_ENERGY_COST;
    private static final ModConfigSpec.LongValue DRACONIC_FUSION_MATRIX_CHARGE_RATE;
    private static final ModConfigSpec.LongValue DRACONIC_FUSION_MATRIX_CAPACITY;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_CRAFT_TICKS;
    private static final ModConfigSpec.BooleanValue DRACONIC_FUSION_MATRIX_ME_CHARGE;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue DRACONIC_FUSION_MATRIX_CHANNELS;
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
    private static final ModConfigSpec.BooleanValue MALUM_OVERRIDE;
    private static final ModConfigSpec.BooleanValue MALUM_SPIRIT_INFUSION;
    private static final ModConfigSpec.BooleanValue MALUM_SPIRIT_FOCUSING;
    private static final ModConfigSpec.BooleanValue MALUM_RUNEWORKING;
    private static final ModConfigSpec.IntValue MALUM_PURE_SPIRIT_DROP_CHANCE;
    private static final ModConfigSpec.IntValue MALUM_REAPING_RITE_KILL_RANGE;
    private static final ModConfigSpec.IntValue MALUM_REAPING_EXECUTION_THRESHOLD;
    private static final ModConfigSpec.IntValue MALUM_REAPING_NETWORK_RANGE;
    private static final ModConfigSpec.IntValue MALUM_REAPING_SWEEP_TICKS;
    private static final ModConfigSpec.BooleanValue MALUM_REAPING_SPARKS;
    private static final ModConfigSpec.BooleanValue MALUM_MATRIX;
    private static final ModConfigSpec.BooleanValue MALUM_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_CHANNELS;
    private static final ModConfigSpec.LongValue MALUM_MATRIX_SPIRIT_STOCK;
    private static final ModConfigSpec.BooleanValue MALUM_MATRIX_OBELISK_SLOT;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_MAX_OBELISKS;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_OBELISK_CRAFT_TICKS;
    private static final ModConfigSpec.BooleanValue MALUM_MATRIX_FOCUSING;
    private static final ModConfigSpec.BooleanValue MALUM_MATRIX_RUNEWORKING;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_IMPETUS_DURABILITY;
    private static final ModConfigSpec.BooleanValue MALUM_MATRIX_CONSUME_IMPETUS;
    private static final ModConfigSpec.BooleanValue MALUM_MATRIX_CATALYZER_SLOT;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_MAX_CATALYZERS;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_CATALYZER_CRAFT_TIME;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_CATALYZER_RESTORATION;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_CATALYZER_CHAIN_FOCUSING;
    private static final ModConfigSpec.IntValue MALUM_MATRIX_CATALYZER_FORTUNE;

    private static final ModConfigSpec.IntValue MYSTICAL_MATRIX_CHANNELS;

    private static final ModConfigSpec.BooleanValue ARS_OVERRIDE;
    private static final ModConfigSpec.BooleanValue ARS_ENCHANTING_APPARATUS;
    private static final ModConfigSpec.BooleanValue ARS_IMBUEMENT_CHAMBER;
    private static final ModConfigSpec.BooleanValue ARS_ARCANE_LECTERN;
    private static final ModConfigSpec.IntValue ARS_ARCANE_LECTERN_MAX_TYPES;
    private static final ModConfigSpec.IntValue ARS_ARCANE_LECTERN_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue ARS_ARCANE_LECTERN_SCRIBES_RANGE;
    private static final ModConfigSpec.BooleanValue ARS_RITUAL_CONDUCTOR;
    private static final ModConfigSpec.IntValue ARS_RITUAL_CONDUCTOR_RANGE;
    private static final ModConfigSpec.IntValue ARS_RITUAL_CONDUCTOR_INTERVAL;
    private static final ModConfigSpec.IntValue ARS_RITUAL_CONDUCTOR_COLLECTION_RADIUS;
    private static final ModConfigSpec.IntValue ARS_RITUAL_CONDUCTOR_IDLE_ME_DRAIN;
    private static final ModConfigSpec.BooleanValue ARS_MATRIX;
    private static final ModConfigSpec.BooleanValue ARS_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue ARS_MATRIX_APPARATUS_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue ARS_MATRIX_IMBUEMENT_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue ARS_MATRIX_SOURCE_JAR_RANGE;
    private static final ModConfigSpec.BooleanValue ARS_MATRIX_ME_SOURCE;
    private static final ModConfigSpec.IntValue ARS_MATRIX_MAX_ACCELERATE;
    private static final ModConfigSpec.IntValue ARS_MATRIX_ACCELERATE_MINIMUM_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue ARS_MATRIX_MAX_DAMPEN;
    private static final ModConfigSpec.IntValue ARS_MATRIX_DAMPEN_SOURCE_DISCOUNT;
    private static final ModConfigSpec.IntValue ARS_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue ARS_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue ARS_MATRIX_CHANNELS;

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
        ACTUALLY_ADDITIONS_MATRIX_ME_CHARGE = builder.comment(
                        "Lets the Matrix charge a craft from its ME network's own power whenever its FE buffer runs short.",
                        "The network's last tenth of stored power is left alone, so a craft drawing its charge cannot brown the network out.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.meNetworkCharge")
                .define("meNetworkCharge", true);
        ACTUALLY_ADDITIONS_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while crafting, in AE per tick. Progress stops whenever the network cannot supply this much power.",
                        "This is separate from the FE it crafts with, and pays for the ME network side of the machine.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        ACTUALLY_ADDITIONS_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        ACTUALLY_ADDITIONS_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.actuallyadditions.atomicEmpoweringMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
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
        COMPACT_CRAFTING_CONTROLLER_CHANNELS = builder.comment(
                        "Channels the Controller takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Controller joins a network, so changing it needs a world reload. Set to 0 for a Controller that takes up no channels at all.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationController.meNetworkChannels")
                .defineInRange("meNetworkChannels", 7, 0, 128);
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
        COMPACT_CRAFTING_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.compactcrafting.miniaturizationMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
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
                        "Spout Filling Module: Automates `create:filling` recipes and Create's generic bucket and bottle filling.",
                        "Covers both machines: a Spout over a Depot, and the Assembly Matrix, which fills without either.")
                .translation("nep.configuration.modules.create.filling")
                .define("filling", true);

        builder.comment("Deployer Module:")
                .translation("nep.configuration.modules.create.deploying")
                .push("deploying");
        CREATE_DEPLOYING = builder.comment(
                        "Automates the Deployer for create:deploying and create:item_application recipes.",
                        "The Depot is the machine: the base item is staged on it and a Deployer two blocks above presses onto it.",
                        "The Assembly Matrix runs the same recipes on its own, with no Depot or Deployer built.")
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
        CREATE_SEQUENCED_ASSEMBLY_CHANNELS = builder.comment(
                        "Channels the Controller takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Controller joins a network, so changing it needs a world reload. Set to 0 for a Controller that takes up no channels at all.")
                .translation("nep.configuration.modules.create.sequencedAssembly.meNetworkChannels")
                .defineInRange("meNetworkChannels", 7, 0, 128);
        builder.pop();

        builder.comment("Assembly Matrix:")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix")
                .push("sequencedAssemblyMatrix");
        CREATE_SEQUENCED_ASSEMBLY_MATRIX = builder.comment(
                        "The Assembly Matrix runs a whole sequenced assembly recipe inside a single block.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.enabled")
                .define("enabled", true);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_GUARANTEED_RESULTS = builder.comment(
                        "Bypasses the output chances of sequenced assembly recipes, so the Matrix craft always yields the recipe's primary result.",
                        "When disabled, the Matrix rolls the recipe's result pool exactly as a physical line does: unlucky rolls produce junk, which is placed into network storage, and the ingredients for that attempt are lost.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.guaranteedResults")
                .define("guaranteedResults", false);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Assembly Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
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
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_ME_CHARGE = builder.comment(
                        "Lets the Matrix pay for an energising step from its ME network's own power whenever its FE buffer runs short.",
                        "The network's last tenth of stored power is left alone, so a craft drawing its charge cannot brown the network out.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.meNetworkCharge")
                .define("meNetworkCharge", true);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Anything above 8 has to connect via a dense cable, and a network with no ME Controller cannot carry more than 8 channels in total.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
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

        builder.comment("Injector Fusion Matrix:")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix")
                .push("fusionMatrix");
        DRACONIC_FUSION_MATRIX = builder.comment(
                        "The Injector Fusion Matrix runs a whole fusion crafting recipe inside a single block, with no Crafting Core or Injectors.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.enabled")
                .define("enabled", true);
        DRACONIC_FUSION_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Injector Fusion Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
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
        DRACONIC_FUSION_MATRIX_ME_CHARGE = builder.comment(
                        "Lets the Matrix charge a fusion craft from its ME network's own power whenever its FE buffer runs short.",
                        "The network's last tenth of stored power is left alone, so a craft drawing its charge cannot brown the network out.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.meNetworkCharge")
                .define("meNetworkCharge", true);
        DRACONIC_FUSION_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while fusing, in AE per tick. Progress stops whenever the network cannot supply this much power.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        DRACONIC_FUSION_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        DRACONIC_FUSION_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
        DRACONIC_FUSION_MATRIX_MAXIMUM_TIER = builder.comment(
                        "Highest fusion recipe tier the Matrix will run: 'draconium', 'wyvern', 'draconic', or 'chaotic'.")
                .translation("nep.configuration.modules.draconicevolution.fusionMatrix.maximumTier")
                .define("maximumTier", "chaotic");

        builder.comment(
                        "Draconic Evolution cores slotted into the Injector Fusion Matrix's upgrade slot tune the machine to the tier of core it holds. Each higher tier compounds lower tiers' upgrades.",
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
        MYSTICAL_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
        builder.pop();

        builder.pop();

        builder.comment("Malum integration.")
                .translation("nep.configuration.modules.malum")
                .push("malum");
        MALUM_OVERRIDE = builder.comment(
                        "Master override for Malum integration.",
                        "When disabled, every Malum module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.malum.override")
                .define("allow_malum_module", true);
        MALUM_SPIRIT_INFUSION = builder.comment(
                        "Spirit Infusion Module: A Spirit Altar with a pattern provider against it takes a whole craft at once, with each recipe component going to the altar or its pedestals in range.",
                        "The altar runs the infusion at its own pace, obelisks included, and the finished item goes back to the pattern provider instead of dropping in-world.")
                .translation("nep.configuration.modules.malum.spiritInfusion")
                .define("spiritInfusion", true);
        MALUM_SPIRIT_FOCUSING = builder.comment(
                        "A Spirit Crucible with a pattern provider against it takes a whole focusing craft at once, with each recipe component going into its slot and the finished node going back to the pattern provider instead of dropping in-world.",
                        "The crucible runs the focusing at its own pace, augments and accelerators included.")
                .translation("nep.configuration.modules.malum.spiritFocusing")
                .define("spiritFocusing", true);
        MALUM_RUNEWORKING = builder.comment(
                        "A Runic Workbench with a pattern provider against it takes a whole rune at once, with the primary ingredient going onto the bench and the second one supplied straight from the network rather than from your hand.",
                        "The workbench shapes the rune at its own pace and the finished rune goes back to the pattern provider instead of dropping in-world.")
                .translation("nep.configuration.modules.malum.runeworking")
                .define("runeworking", true);
        MALUM_PURE_SPIRIT_DROP_CHANCE = builder.comment(
                        "Percent chance for a Pure Spirit to come out of a soul harvest on an entity in the nep:pure_spirit_sources tag.",
                        "Set to 0 to stop Pure Spirits dropping altogether.")
                .translation("nep.configuration.modules.malum.pureSpiritDropChance")
                .defineInRange("pureSpiritDropChance", 5, 0, 100);

        builder.comment(
                        "The Rite of Reaping, a corrupted greater rite that finishes off wounded monsters and feeds the spirits they drop straight into an ME network.")
                .translation("nep.configuration.modules.malum.riteOfReaping")
                .push("riteOfReaping");
        MALUM_REAPING_RITE_KILL_RANGE = builder.comment(
                        "Blocks from the totem base the rite reaches for monsters, and the same radius it sweeps for loose spirit shards afterwards.")
                .translation("nep.configuration.modules.malum.riteOfReaping.reapingRiteKillRange")
                .defineInRange("reapingRiteKillRange", 8, 1, 64);
        MALUM_REAPING_EXECUTION_THRESHOLD = builder.comment(
                        "Health percentage a monster must be at or under before the rite executes it.")
                .translation("nep.configuration.modules.malum.riteOfReaping.executionThreshold")
                .defineInRange("executionThresholdPercent", 25, 1, 100);
        MALUM_REAPING_NETWORK_RANGE = builder.comment(
                        "Blocks from the totem base the rite searches for an ME network to hand the harvested spirits to.",
                        "Only chunks already in memory are checked, so a rite never loads terrain to find a network.")
                .translation("nep.configuration.modules.malum.riteOfReaping.networkRange")
                .defineInRange("networkRange", 8, 1, 64);
        MALUM_REAPING_SWEEP_TICKS = builder.comment(
                        "Ticks between the rite's sweeps for monsters to execute. Malum's own rites sweep every 100.")
                .translation("nep.configuration.modules.malum.riteOfReaping.sweepTicks")
                .defineInRange("sweepTicks", 20, 1, 200);
        MALUM_REAPING_SPARKS = builder.comment(
                        "Whether the rite sends a spark flying at every monster it claims, the way Malum's own rites do.",
                        "Turn off on mob-heavy packs: the blow then lands the moment a sweep finds the monster, with no spark entity to spawn or track.")
                .translation("nep.configuration.modules.malum.riteOfReaping.sparks")
                .define("sparks", true);
        builder.pop();

        builder.comment("Focused Spirit Matrix:")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix")
                .push("focusedSpiritMatrix");
        MALUM_MATRIX = builder.comment(
                        "The Focused Spirit Matrix runs spirit infusion recipes inside a single block.",
                        "Its input buffer stands in for the spirit slots and the pedestals a recipe would otherwise need.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.enabled")
                .define("enabled", true);
        MALUM_MATRIX_RUNEWORKING = builder.comment(
                        "Whether the Matrix shapes runes as well. Off leaves it refusing runeworking patterns outright so the network can look for a real Runic Workbench instead.",
                        "This ONLY affects the Matrix. The Runeworking module above, which is a pattern provider against a real Runic Workbench, has its own config.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.runeworking")
                .define("runeworking", true);
        MALUM_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Matrix left short of materials stalls until you restock it by hand.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.autoRequest")
                .define("autoRequest", true);
        MALUM_MATRIX_CRAFT_TICKS = builder.comment(
                        "Ticks the Matrix spends on a craft. 20 ticks is one second.",
                        "A Spirit Altar caps at 300 ticks, but only its first craft pays that in full: a repeat craft resumes at 80% and spends 60 ticks fetching each extra ingredient, which averages about 180 ticks across Malum's infusions.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.craftTicks")
                .defineInRange("craftTicks", 90, 1, 24_000);
        MALUM_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while crafting, in AE per tick. Progress stops whenever the network cannot supply this much power.",
                        "Spirit infusion has no energy cost of its own, so this is the whole running cost of the machine.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        MALUM_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        MALUM_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
        MALUM_MATRIX_SPIRIT_STOCK = builder.comment(
                        "Spirits per kind the Matrix keeps in its spirit bank while Restock Spirits is on, in item quantity.",
                        "The bank is network storage, so the shards it holds count toward a craft the moment they are in it, and restocking only ever pulls from somewhere else on the network.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.spiritStock")
                .defineInRange("spiritStock", 64L, 1L, 1024L);

        builder.comment(
                        "Malum Obelisks slotted into the Focused Spirit Matrix's upgrade slot speed its infusions up, the way they accelerate a real Spirit Altars.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.upgrades")
                .push("upgrades");
        MALUM_MATRIX_OBELISK_SLOT = builder.comment(
                        "Whether the Matrix offers an obelisk upgrade slot at all. Off closes the slot and leaves infusion running at craftTicks.",
                        "Independent of the catalyzer slot.",
                        "Obelisks already installed when this is turned off stop counting but can still be taken back out.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.upgrades.obeliskSlot")
                .define("obeliskSlot", true);
        MALUM_MATRIX_MAX_OBELISKS = builder.comment(
                        "Quantity of Runewood Obelisks accepted, and the count a full slot is measured against. Lowering this makes each obelisk better rather than lowering the ceiling, since a full slot always reaches obeliskMinimumCraftTicks.",
                        "To close the slot entirely, use obeliskSlot above.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.upgrades.maxObelisks")
                .defineInRange("maxObelisks", 32, 1, 64);
        MALUM_MATRIX_OBELISK_CRAFT_TICKS = builder.comment(
                        "Crafting time a full slot of Runewood Obelisks reaches, in ticks. Upgrades scales linearly from craftTicks down to this floor, following maxObelisks.",
                        "Values above craftTicks are ignored, so obelisks never slow a Matrix down.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.upgrades.obeliskCraftTicks")
                .defineInRange("obeliskMinimumCraftTicks", 5, 1, 24_000);
        builder.pop();

        builder.comment(
                        "Spirit focusing in the Matrix, which needs a Matrix Impetus in its impetus slot and is sped up by Matrix Catalyzers.",
                        "A Matrix Impetus stands in for every one of Malum's, so a single Matrix focuses every node rather than one Matrix per metal.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing")
                .push("focusing");
        MALUM_MATRIX_FOCUSING = builder.comment(
                        "Whether the Matrix runs any Spirit Focusing at all. Off leaves it an infusion-only machine: it refuses spirit focusing patterns outright so the network can look elsewhere, and closes both the catalyzer and the impetus slots.",
                        "This ONLY affects the Matrix. The Spirit Focusing module above, which is a pattern provider against a real Spirit Crucible, has its own config.",
                        "Catalyzers and an impetus already installed when this is turned off stop counting but can still be taken back out.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.enabled")
                .define("enabled", true);
        MALUM_MATRIX_IMPETUS_DURABILITY = builder.comment("Durability of a Matrix Impetus before it is spent.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.impetusDurability")
                .defineInRange("impetusDurability", 1_600, 1, 100_000);
        MALUM_MATRIX_CONSUME_IMPETUS = builder.comment(
                        "Whether focusing in the Matrix spends the Matrix Impetus' durability, the way a real Spirit Crucible cracks its impetus.",
                        "On by default, which is what gives the catalyzers' restoration something to repair. The craft that spends the last of an impetus still finishes and leaves a Fractured Matrix Impetus in the slot, which a Repair Pylon puts back together.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.consumeImpetusDurability")
                .define("consumeImpetusDurability", true);
        MALUM_MATRIX_CATALYZER_SLOT = builder.comment(
                        "Whether the Matrix offers a catalyzer slot at all. Off closes the slot and leaves focusing running at the recipe's own speed with none of the effects below.",
                        "Catalyzers already installed when this is turned off stop counting but can still be taken back out.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.catalyzerSlot")
                .define("catalyzerSlot", true);
        MALUM_MATRIX_MAX_CATALYZERS = builder.comment(
                        "Quantity of Matrix Catalyzers accepted, which defines the 'value per upgrade' curve. Every cap below is what a full slot reaches, so one catalyzer is worth its cap divided by this.",
                        "Lowering this makes each catalyzer better without changing what a full slot is worth. To switch a single effect off, set its own cap to 0; to close the slot entirely, use catalyzerSlot above.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.maxCatalyzers")
                .defineInRange("maxCatalyzers", 4, 1, 64);
        MALUM_MATRIX_CATALYZER_CRAFT_TIME = builder.comment(
                        "Focusing time a full slot of Matrix Catalyzers can cut, as a percentage of the recipe's own time. Upgrades scales linearly toward this cap.",
                        "At 100 a full slot finishes any focusing craft in a single tick.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.catalyzerCraftTime")
                .defineInRange("catalyzerCraftTimeReductionPercent", 75, 0, 100);
        MALUM_MATRIX_CATALYZER_RESTORATION = builder.comment(
                        "Chance a full slot of Matrix Catalyzers repairs the Matrix Impetus after a focusing craft that spent durability, as a percentage. Upgrades scales linearly toward this cap.",
                        "One success mends 1% of the impetus' maximum durability, matching Malum's Mending Diffuser. Above 100 the surplus is a further guaranteed repair, so 250 mends twice over with a 50% chance at a third.",
                        "Does nothing while consumeImpetusDurability is off, since an impetus that never wears has nothing to mend.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.catalyzerRestoration")
                .defineInRange("catalyzerRestorationPercent", 50, 0, 1000);
        MALUM_MATRIX_CATALYZER_CHAIN_FOCUSING = builder.comment(
                        "Chance a full slot of Matrix Catalyzers chains one focusing craft straight into the next, as a percentage. Upgrades scales linearly toward this cap.",
                        "A chained craft still claims its own ingredients and impetus durability; it only restarts near enough completion that it finishes in "
                                + CHAIN_FOCUSING_REMAINDER_TICKS
                                + " ticks, the way Malum's Warping Engine skips a crucible forward.",
                        "Capped at 99% to avoid infinite-looping crafts: at 100% every craft would chain into the next one forever.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.catalyzerChainFocusing")
                .defineInRange("catalyzerChainFocusingPercent", 25, 0, 99);
        MALUM_MATRIX_CATALYZER_FORTUNE = builder.comment(
                        "Chance a full slot of Matrix Catalyzers yields a second copy of a focusing craft's output, as a percentage. Upgrades scales linearly toward this cap.",
                        "Above 100 the surplus is a further guaranteed copy, so at 250% it always yields two extra with a 50% chance at a third, matching how a Spirit Crucible rolls fortune.",
                        "Bonus output is returned to the network alongside what was ordered, so an AE2 crafting job takes what it asked for and the rest lands in storage.")
                .translation("nep.configuration.modules.malum.focusedSpiritMatrix.focusing.catalyzerFortune")
                .defineInRange("catalyzerFortunePercent", 25, 0, 1000);
        builder.pop();

        builder.pop();

        builder.pop();

        builder.comment("Ars Nouveau integration.")
                .translation("nep.configuration.modules.ars_nouveau")
                .push("ars_nouveau");
        ARS_OVERRIDE = builder.comment(
                        "Master override for Ars Nouveau integration.",
                        "When disabled, every Ars Nouveau module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.ars_nouveau.override")
                .define("allow_ars_nouveau_module", true);
        ARS_ENCHANTING_APPARATUS = builder.comment(
                        "Enchanting Apparatus Module: An Enchanting Apparatus with a pattern provider against it takes a whole craft at once, with the reagent going into the apparatus and one ingredient onto each surrounding Arcane Pedestal.",
                        "The apparatus starts itself but does NOT automatically return the finished product.",
                        "Only plain enchanting_apparatus recipes are driven. Enchantment, armour upgrade, prestidigitation and spell write recipes derive their result from the reagent's own data, so they can never be encoded as a fixed pattern.")
                .translation("nep.configuration.modules.ars_nouveau.enchantingApparatus")
                .define("enchantingApparatus", true);
        ARS_IMBUEMENT_CHAMBER = builder.comment(
                        "Imbuement Chamber Module: An Imbuement Chamber with a pattern provider against it takes a whole craft at once, with the reagent going into the chamber and one ingredient onto each Arcane Pedestal touching it.",
                        "The chamber draws its own source and runs the craft itself, but does NOT automatically return the finished product.",
                        "Only plain imbuement recipes are driven. Addons register their own imbuement recipe types for things like charging a charm or writing a scroll, and those derive their result from the reagent, so they can never be encoded as a fixed pattern.")
                .translation("nep.configuration.modules.ars_nouveau.imbuementChamber")
                .define("imbuementChamber", true);

        builder.comment("Arcane Lectern:")
                .translation("nep.configuration.modules.ars_nouveau.arcaneLectern")
                .push("arcaneLectern");
        ARS_ARCANE_LECTERN = builder.comment(
                        "The Arcane Lectern presents its ME network to Ars Nouveau the way a Storage Lectern presents its linked chests, so anything that pulls from a neighbouring inventory can draw straight out of the network.",
                        "A Scribes Table is served differently: the Lectern sends a table only the glyph reagents it is still missing, and only while it asks for them.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneLectern.enabled")
                .define("enabled", true);
        ARS_ARCANE_LECTERN_MAX_TYPES = builder.comment(
                        "Most distinct item types the Lectern exposes to a neighbouring machine at once.",
                        "Machines such as the Wixie Cauldron walk every exposed slot when they look for an ingredient, so a large network is cheaper to serve with a lower cap. Items beyond the cap are not offered. Scribes Tables are not affected, since the Lectern sends them their reagents directly.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneLectern.maximumItemTypes")
                .defineInRange("maximumItemTypes", 1_024, 1, 65_536);
        ARS_ARCANE_LECTERN_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Lectern draws from its ME network while idle, in AE per tick. This standing cost is read once when the Lectern joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneLectern.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        ARS_ARCANE_LECTERN_SCRIBES_RANGE = builder.comment(
                        "How far, in blocks along each axis, a Scribes Table can be from the Lectern and still be sent its glyph reagents.",
                        "The Lectern only sends while a table is asking for reagents, so a longer range costs nothing between crafts.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneLectern.scribesTableRange")
                .defineInRange("scribesTableRange", 8, 1, 32);
        builder.pop();

        builder.comment("Ritual Conductor:")
                .translation("nep.configuration.modules.ars_nouveau.ritualConductor")
                .push("ritualConductor");
        ARS_RITUAL_CONDUCTOR = builder.comment(
                        "The Ritual Conductor keeps a nearby Ritual Brazier running by itself, drawing a ritual tablet out of the ME network, feeding the augments it was configured with, and lighting the ritual once its conditions are met.",
                        "Almost every ritual is a world effect rather than a craft, so the Conductor is a trigger rather than a pattern target: it has no output and takes no pattern.",
                        "Source is still the Brazier's own problem, drawn from source jars within 6 blocks of it as usual.")
                .translation("nep.configuration.modules.ars_nouveau.ritualConductor.enabled")
                .define("enabled", true);
        ARS_RITUAL_CONDUCTOR_RANGE = builder.comment(
                        "How far the Conductor looks for a Ritual Brazier, in blocks, as a cube radius around itself. The nearest Brazier found wins.")
                .translation("nep.configuration.modules.ars_nouveau.ritualConductor.brazierRange")
                .defineInRange("brazierRange", 4, 1, 16);
        ARS_RITUAL_CONDUCTOR_INTERVAL = builder.comment(
                        "Ticks between Conductor passes. One pass arms the Brazier, feeds every outstanding augment, and tries to start the ritual.",
                        "A Conductor that cannot see a Brazier waits five times this long before looking again.")
                .translation("nep.configuration.modules.ars_nouveau.ritualConductor.interval")
                .defineInRange("interval", 20, 1, 24_000);
        ARS_RITUAL_CONDUCTOR_COLLECTION_RADIUS = builder.comment(
                        "How far around the Brazier the Conductor sweeps up what a ritual dropped, in blocks. Set to 0 to leave dropped items alone.",
                        "The sweep runs once, at the moment a ritual finishes, rather than on a timer, so its cost scales with rituals completed and not with time spent waiting.",
                        "Items a player threw are never swept, so a Conductor cannot steal what you drop next to the Brazier.")
                .translation("nep.configuration.modules.ars_nouveau.ritualConductor.collectionRadius")
                .defineInRange("collectionRadius", 4, 0, 16);
        ARS_RITUAL_CONDUCTOR_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Conductor draws from its ME network while idle, in AE per tick. This standing cost is read once when the Conductor joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.ars_nouveau.ritualConductor.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        builder.pop();

        builder.comment("Arcane Enchanting Matrix:")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix")
                .push("arcaneEnchantingMatrix");
        ARS_MATRIX = builder.comment(
                        "The Arcane Enchanting Matrix runs whole Enchanting Apparatus and Imbuement Chamber recipes inside a single block, paying for them from its own Source store.",
                        "Imbuement pedestal items are borrowed from the ME network for as long as a job still needs them, then handed back.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.enabled")
                .define("enabled", true);
        ARS_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients and imbuement pedestal items from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Matrix left short of materials stalls until you manually restock it.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.autoRequest")
                .define("autoRequest", true);
        ARS_MATRIX_APPARATUS_CRAFT_TICKS = builder.comment(
                        "Ticks the Matrix spends on an Enchanting Apparatus craft before any Accelerate glyphs. 20 ticks is one second.",
                        "The real Enchanting Apparatus takes 210 ticks.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.apparatusCraftTicks")
                .defineInRange("apparatusCraftTicks", 210, 1, 24_000);
        ARS_MATRIX_IMBUEMENT_CRAFT_TICKS = builder.comment(
                        "Ticks the Matrix spends on an imbuement craft before any Accelerate glyphs. 20 ticks is one second.",
                        "The real Imbuement Chamber takes 100 ticks once it has its Source.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.imbuementCraftTicks")
                .defineInRange("imbuementCraftTicks", 100, 1, 24_000);
        ARS_MATRIX_SOURCE_JAR_RANGE = builder.comment(
                        "How far the Matrix reaches for Source Jars, in blocks, measured the way Ars Nouveau measures it for the Enchanting Apparatus.",
                        "The Matrix only draws what its queued crafts still need, so it never drains nearby jars for a store it has no use for.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.sourceJarRange")
                .defineInRange("sourceJarRange", 10, 2, 32);
        ARS_MATRIX_ME_SOURCE = builder.comment(
                        "Lets the Matrix draw the Source its queued crafts need out of its ME network when Ars Énergistique is installed.",
                        "Nearby Source Jars are drawn from first.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.meNetworkSource")
                .define("meNetworkSource", true);
        ARS_MATRIX_MAX_ACCELERATE = builder.comment(
                        "Most Accelerate glyphs the Matrix's speed slot holds. The full stack reaches the minimum craft time below.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.maxAccelerateGlyphs")
                .defineInRange("maxAccelerateGlyphs", 16, 1, 64);
        ARS_MATRIX_ACCELERATE_MINIMUM_CRAFT_TICKS = builder.comment(
                        "Craft time a full stack of Accelerate glyphs brings every recipe down to, in ticks. Fewer glyphs land proportionally between this and the base time.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.accelerateMinimumCraftTicks")
                .defineInRange("accelerateMinimumCraftTicks", 10, 1, 24_000);
        ARS_MATRIX_MAX_DAMPEN = builder.comment(
                        "Most Dampen glyphs the Matrix's efficiency slot holds. The full stack gives the whole Source discount below.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.maxDampenGlyphs")
                .defineInRange("maxDampenGlyphs", 16, 1, 64);
        ARS_MATRIX_DAMPEN_SOURCE_DISCOUNT = builder.comment(
                        "Source a full stack of Dampen glyphs takes off every craft, as a percentage of the recipe's own cost. Fewer glyphs give a proportional share.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.dampenSourceDiscount")
                .defineInRange("dampenSourceDiscountPercent", 50, 0, 100);
        ARS_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while crafting, in AE per tick. Progress stops whenever the network cannot supply this much power.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        ARS_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        ARS_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.ars_nouveau.arcaneEnchantingMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
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

        builder.comment("Machine behaviour outside any one integration.")
                .translation("nep.configuration.machines")
                .push("machines");
        MANUAL_CRAFTING = builder.comment(
                        "Lets a recipe be started by hand from a machine's own screen, by pressing the recipe-viewer transfer button on a recipe that machine runs.",
                        "The ingredients come out of your own inventory, all at once or not at all, and the result waits in the machine's output buffer instead of going back to the ME network.",
                        "When disabled, a machine only ever crafts what a pattern provider pushes to it.")
                .translation("nep.configuration.machines.manualCrafting")
                .define("manualCrafting", true);
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
                .defineInRange("scanBudget", 128, 4, 1024);
        MACHINE_HUB_CASING_DEPTH = builder.comment(
                        "How many plain blocks in a row the Machine Hub's scan may step through between one machine part and the next.",
                        "The scan travels freely through blocks carrying a block entity, which is every hatch, port and controller. Everything else is casing, and casing is only crossed for this many blocks at a time, and only when it belongs to a mod that already owns a machine part nearby. Raise it for a multiblock with thick walls between its hatches; set it to zero to have the scan follow machine parts alone.")
                .translation("nep.configuration.machineHub.casingDepth")
                .defineInRange("casingDepth", 3, 0, 8);
        MACHINE_HUB_SCAN_WHITELIST = builder.comment(
                        "Blocks the Machine Hub's scan always treats as machine parts, written as block ids (modid:block) or mod id regex (modid:*).",
                        "Whatever stands against a whitelisted block is trusted the way blocks touching the Hub are, and a whitelisted casing is crossed without counting against casingDepth.",
                        "The block tag nep:machine_hub/parts does the same thing in a datapack.")
                .translation("nep.configuration.machineHub.scanWhitelist")
                .defineListAllowEmpty("scanWhitelist", List.of(), () -> "minecraft:stone", HubRules::configEntry);
        MACHINE_HUB_SCAN_BLACKLIST = builder.comment(
                        "Blocks the Machine Hub's scan never walks through and never proposes, written as block ids (modid:block) or mod id regex (modid:*). The blacklist wins over the whitelist.",
                        "The block tag nep:machine_hub/blocked does the same thing in a datapack, and ships with common player storage (chests, barrels, shulker boxes) and general terrain and building blocks (dirt, stone, sand, wood, wool, concrete) already in it.")
                .translation("nep.configuration.machineHub.scanBlacklist")
                .defineListAllowEmpty("scanBlacklist", List.of(), () -> "minecraft:stone", HubRules::configEntry);
        MACHINE_HUB_LINK_BLACKLIST = builder.comment(
                        "Blocks a Machine Hub refuses to link at all, even from a Hub Linker plan, written as block ids (modid:block) or a mod id regex (modid:*).",
                        "The scan can still go through them to whatever lies beyond. The block tag nep:machine_hub/unlinkable does the same thing in a datapack.")
                .translation("nep.configuration.machineHub.linkBlacklist")
                .defineListAllowEmpty("linkBlacklist", List.of(), () -> "minecraft:stone", HubRules::configEntry);
        MACHINE_HUB_CHANNELS = builder.comment(
                        "Channels the Machine Hub takes up on its ME network before its links are counted. Each linked inventory adds meNetworkChannelsPerLink on top, and anything above 8 has to pass through dense cables.",
                        "This is read once when the Hub joins a network, so changing it needs a world reload. Set both this and meNetworkChannelsPerLink to 0 for a Hub that takes up no channels at all.")
                .translation("nep.configuration.machineHub.meNetworkChannels")
                .defineInRange("meNetworkChannels", 11, 0, 128);
        MACHINE_HUB_CHANNELS_PER_LINK = builder.comment(
                        "Channels each inventory linked to the Machine Hub adds to what the Hub takes up, standing in for a bus each hatch would otherwise need.",
                        "A Hub can take up to the channel limit at most depending on your AE2 config. Set to 0 to have links cost nothing.")
                .translation("nep.configuration.machineHub.meNetworkChannelsPerLink")
                .defineInRange("meNetworkChannelsPerLink", 1, 0, 128);
        builder.pop();

        builder.comment("Pattern Decoder behaviour.")
                .translation("nep.configuration.patternDecoder")
                .push("patternDecoder");
        REQUIRE_DECODER = builder.comment(
                        "Makes NEP patterns depend on a Pattern Decoder. While enabled, a Pattern Encoding Terminal only encodes a NEP pattern for a mod switched on under mods when its network holds a powered Pattern Decoder carrying that mod's Encoding Module; anything else encodes as a plain Processing Pattern.",
                        "When disabled, NEP patterns encode as usual and the Pattern Decoder does nothing. Patterns that were already encoded keep working either way.")
                .translation("nep.configuration.patternDecoder.requireDecoder")
                .define("requireDecoder", false);
        PATTERN_DECODER_CHANNELS = builder.comment(
                        "Channels a Pattern Decoder takes up on its ME network. A Decoder without its channel does not count towards encoding.",
                        "This is read once when the Decoder joins a network, so changing it needs a world reload. Set to 0 for a Decoder that takes up no channel at all.")
                .translation("nep.configuration.patternDecoder.meNetworkChannels")
                .defineInRange("meNetworkChannels", 1, 0, 128);
        PATTERN_DECODER_IDLE_ME_DRAIN = builder.comment(
                        "Energy a Pattern Decoder draws from its ME network, in AE per tick. This standing cost is read once when the Decoder joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.patternDecoder.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 5, 0, 1_000_000);
        builder.comment(
                        "Which integrations need their Encoding Module in a Pattern Decoder while requireDecoder is enabled. A mod switched off here encodes its NEP patterns as usual.")
                .translation("nep.configuration.patternDecoder.mods")
                .push("mods");
        for (DecoderModule module : DecoderModule.values()) {
            DECODER_MODULES.put(
                    module,
                    builder.comment("Whether " + module.modId() + " patterns need their Encoding Module.")
                            .translation("nep.configuration.patternDecoder.mods." + module.modId())
                            .define(module.modId(), true));
        }
        builder.pop();
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
                    "Assembly Matrix idleMeNetworkDrain ({} AE/t) is above meNetworkDrain ({} AE/t); the idle"
                            + " drain is clamped to {} AE/t, so assembling costs no more than sitting idle.",
                    idleDrain,
                    activeDrain,
                    activeDrain);
        }
        int minimumStress = CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS_MINIMUM.get();
        int maximumStress = CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS.get();
        if (minimumStress > maximumStress) {
            Nep.LOGGER.warn(
                    "Assembly Matrix stressUnitsMinimum ({} SU) is above stressUnits ({} SU); the floor is"
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

    public static boolean manualCrafting() {
        return !SPEC.isLoaded() || MANUAL_CRAFTING.get();
    }

    public static boolean processingPatternConversion() {
        return SPEC.isLoaded() && PROCESSING_PATTERN_CONVERSION.get();
    }

    public static boolean requireDecoder() {
        return SPEC.isLoaded() && REQUIRE_DECODER.get();
    }

    public static boolean decoderRequiredFor(DecoderModule module) {
        return requireDecoder() && DECODER_MODULES.get(module).get();
    }

    public static int patternDecoderChannels() {
        return SPEC.isLoaded() ? PATTERN_DECODER_CHANNELS.get() : 1;
    }

    public static int patternDecoderIdleMeDrain() {
        return SPEC.isLoaded() ? PATTERN_DECODER_IDLE_ME_DRAIN.get() : 5;
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
        return SPEC.isLoaded() ? MACHINE_HUB_SCAN_BUDGET.get() : 128;
    }

    public static int machineHubCasingDepth() {
        return SPEC.isLoaded() ? MACHINE_HUB_CASING_DEPTH.get() : 3;
    }

    public static List<? extends String> machineHubScanWhitelist() {
        return SPEC.isLoaded() ? MACHINE_HUB_SCAN_WHITELIST.get() : List.of();
    }

    public static List<? extends String> machineHubScanBlacklist() {
        return SPEC.isLoaded() ? MACHINE_HUB_SCAN_BLACKLIST.get() : List.of();
    }

    public static List<? extends String> machineHubLinkBlacklist() {
        return SPEC.isLoaded() ? MACHINE_HUB_LINK_BLACKLIST.get() : List.of();
    }

    public static int machineHubChannels() {
        return SPEC.isLoaded() ? MACHINE_HUB_CHANNELS.get() : 11;
    }

    public static int machineHubChannelsPerLink() {
        return SPEC.isLoaded() ? MACHINE_HUB_CHANNELS_PER_LINK.get() : 1;
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

    public static boolean actuallyAdditionsMatrixMeCharge() {
        return !SPEC.isLoaded() || ACTUALLY_ADDITIONS_MATRIX_ME_CHARGE.get();
    }

    public static int actuallyAdditionsMatrixMeDrain() {
        return SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int actuallyAdditionsMatrixIdleMeDrain() {
        int idle = SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, actuallyAdditionsMatrixMeDrain());
    }

    public static int actuallyAdditionsMatrixChannels() {
        return SPEC.isLoaded() ? ACTUALLY_ADDITIONS_MATRIX_CHANNELS.get() : 15;
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

    public static int compactCraftingMatrixChannels() {
        return SPEC.isLoaded() ? COMPACT_CRAFTING_MATRIX_CHANNELS.get() : 15;
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

    public static int compactCraftingControllerChannels() {
        return SPEC.isLoaded() ? COMPACT_CRAFTING_CONTROLLER_CHANNELS.get() : 7;
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
        return SPEC.isLoaded() && CREATE_SEQUENCED_ASSEMBLY_MATRIX_GUARANTEED_RESULTS.get();
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

    public static int createSequencedAssemblyMatrixChannels() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_CHANNELS.get() : 15;
    }

    public static int createSequencedAssemblyChannels() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_CHANNELS.get() : 7;
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

    public static boolean createSequencedAssemblyMatrixMeCharge() {
        return !SPEC.isLoaded() || CREATE_SEQUENCED_ASSEMBLY_MATRIX_ME_CHARGE.get();
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

    public static boolean draconicFusionMatrixMeCharge() {
        return !SPEC.isLoaded() || DRACONIC_FUSION_MATRIX_ME_CHARGE.get();
    }

    public static int draconicFusionMatrixMeDrain() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int draconicFusionMatrixIdleMeDrain() {
        int idle = SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, draconicFusionMatrixMeDrain());
    }

    public static int draconicFusionMatrixChannels() {
        return SPEC.isLoaded() ? DRACONIC_FUSION_MATRIX_CHANNELS.get() : 15;
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

    public static boolean malumOverride() {
        return SPEC.isLoaded() && MALUM_OVERRIDE.get();
    }

    public static boolean malumSpiritInfusion() {
        return malumOverride() && MALUM_SPIRIT_INFUSION.get();
    }

    public static boolean malumRuneworking() {
        return malumOverride() && MALUM_RUNEWORKING.get();
    }

    public static boolean malumSpiritFocusing() {
        return malumOverride() && MALUM_SPIRIT_FOCUSING.get();
    }

    public static int malumPureSpiritDropChance() {
        return malumOverride() ? MALUM_PURE_SPIRIT_DROP_CHANCE.get() : 5;
    }

    public static int malumReapingreapingRiteKillRange() {
        return MALUM_REAPING_RITE_KILL_RANGE.get();
    }

    public static float malumReapingExecutionThreshold() {
        return MALUM_REAPING_EXECUTION_THRESHOLD.get() / 100.0F;
    }

    public static int malumReapingNetworkRange() {
        return MALUM_REAPING_NETWORK_RANGE.get();
    }

    public static int malumReapingSweepTicks() {
        return MALUM_REAPING_SWEEP_TICKS.get();
    }

    public static boolean malumReapingSparks() {
        return MALUM_REAPING_SPARKS.get();
    }

    public static boolean malumFocusedSpiritMatrix() {
        return malumOverride() && MALUM_MATRIX.get();
    }

    public static boolean malumFocusedSpiritMatrixAutoRequest() {
        return malumFocusedSpiritMatrix() && MALUM_MATRIX_AUTO_REQUEST.get();
    }

    public static int malumFocusedSpiritMatrixCraftTicks() {
        return SPEC.isLoaded() ? MALUM_MATRIX_CRAFT_TICKS.get() : 90;
    }

    public static int malumFocusedSpiritMatrixMeDrain() {
        return SPEC.isLoaded() ? MALUM_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int malumFocusedSpiritMatrixIdleMeDrain() {
        int idle = SPEC.isLoaded() ? MALUM_MATRIX_IDLE_ME_DRAIN.get() : 10;
        return Math.min(idle, malumFocusedSpiritMatrixMeDrain());
    }

    public static int malumFocusedSpiritMatrixChannels() {
        return SPEC.isLoaded() ? MALUM_MATRIX_CHANNELS.get() : 15;
    }

    public static long malumFocusedSpiritMatrixSpiritStock() {
        return SPEC.isLoaded() ? MALUM_MATRIX_SPIRIT_STOCK.get() : 64L;
    }

    public static boolean malumFocusedSpiritMatrixObeliskSlot() {
        return !SPEC.isLoaded() || MALUM_MATRIX_OBELISK_SLOT.get();
    }

    public static int malumFocusedSpiritMatrixMaxObelisks() {
        return SPEC.isLoaded() ? MALUM_MATRIX_MAX_OBELISKS.get() : 32;
    }

    public static boolean malumFocusedSpiritMatrixRuneworking() {
        return malumFocusedSpiritMatrix() && MALUM_MATRIX_RUNEWORKING.get();
    }

    public static boolean malumFocusedSpiritMatrixFocusing() {
        return malumFocusedSpiritMatrix() && MALUM_MATRIX_FOCUSING.get();
    }

    public static int malumFocusedSpiritMatrixImpetusDurability() {
        return SPEC.isLoaded() ? MALUM_MATRIX_IMPETUS_DURABILITY.get() : 1_600;
    }

    public static boolean malumFocusedSpiritMatrixConsumeImpetusDurability() {
        return !SPEC.isLoaded() || MALUM_MATRIX_CONSUME_IMPETUS.get();
    }

    public static boolean malumFocusedSpiritMatrixCatalyzerSlot() {
        return !SPEC.isLoaded() || MALUM_MATRIX_CATALYZER_SLOT.get();
    }

    public static int malumFocusedSpiritMatrixMaxCatalyzers() {
        return SPEC.isLoaded() ? MALUM_MATRIX_MAX_CATALYZERS.get() : 4;
    }

    public static int malumFocusedSpiritMatrixCatalyzerCraftTimeReductionPercent() {
        return SPEC.isLoaded() ? MALUM_MATRIX_CATALYZER_CRAFT_TIME.get() : 75;
    }

    public static int malumFocusedSpiritMatrixCatalyzerRestorationPercent() {
        return SPEC.isLoaded() ? MALUM_MATRIX_CATALYZER_RESTORATION.get() : 50;
    }

    public static int malumFocusedSpiritMatrixCatalyzerChainFocusingPercent() {
        return SPEC.isLoaded() ? MALUM_MATRIX_CATALYZER_CHAIN_FOCUSING.get() : 25;
    }

    public static int malumFocusedSpiritMatrixCatalyzerFortunePercent() {
        return SPEC.isLoaded() ? MALUM_MATRIX_CATALYZER_FORTUNE.get() : 25;
    }

    public static int malumFocusedSpiritMatrixObeliskMinimumCraftTicks() {
        int floor = SPEC.isLoaded() ? MALUM_MATRIX_OBELISK_CRAFT_TICKS.get() : 5;
        return Math.min(floor, malumFocusedSpiritMatrixCraftTicks());
    }

    public static int mysticalInfusedAwakeningMatrixChannels() {
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_CHANNELS.get() : 15;
    }

    public static boolean arsOverride() {
        return SPEC.isLoaded() && ARS_OVERRIDE.get();
    }

    public static boolean arsEnchantingApparatus() {
        return arsOverride() && ARS_ENCHANTING_APPARATUS.get();
    }

    public static boolean arsImbuementChamber() {
        return arsOverride() && ARS_IMBUEMENT_CHAMBER.get();
    }

    public static boolean arsArcaneLectern() {
        return arsOverride() && ARS_ARCANE_LECTERN.get();
    }

    public static int arsArcaneLecternMaxTypes() {
        return SPEC.isLoaded() ? ARS_ARCANE_LECTERN_MAX_TYPES.get() : 1_024;
    }

    public static int arsArcaneLecternIdleMeDrain() {
        return SPEC.isLoaded() ? ARS_ARCANE_LECTERN_IDLE_ME_DRAIN.get() : 10;
    }

    public static int arsArcaneLecternScribesRange() {
        return SPEC.isLoaded() ? ARS_ARCANE_LECTERN_SCRIBES_RANGE.get() : 8;
    }

    public static boolean arsRitualConductor() {
        return arsOverride() && ARS_RITUAL_CONDUCTOR.get();
    }

    public static int arsRitualConductorRange() {
        return SPEC.isLoaded() ? ARS_RITUAL_CONDUCTOR_RANGE.get() : 4;
    }

    public static int arsRitualConductorInterval() {
        return SPEC.isLoaded() ? ARS_RITUAL_CONDUCTOR_INTERVAL.get() : 20;
    }

    public static int arsRitualConductorCollectionRadius() {
        return SPEC.isLoaded() ? ARS_RITUAL_CONDUCTOR_COLLECTION_RADIUS.get() : 4;
    }

    public static int arsRitualConductorIdleMeDrain() {
        return SPEC.isLoaded() ? ARS_RITUAL_CONDUCTOR_IDLE_ME_DRAIN.get() : 10;
    }

    public static boolean arsMatrix() {
        return arsOverride() && ARS_MATRIX.get();
    }

    public static boolean arsMatrixAutoRequest() {
        return arsMatrix() && ARS_MATRIX_AUTO_REQUEST.get();
    }

    public static int arsMatrixApparatusCraftTicks() {
        return SPEC.isLoaded() ? ARS_MATRIX_APPARATUS_CRAFT_TICKS.get() : 210;
    }

    public static int arsMatrixImbuementCraftTicks() {
        return SPEC.isLoaded() ? ARS_MATRIX_IMBUEMENT_CRAFT_TICKS.get() : 100;
    }

    public static int arsMatrixSourceJarRange() {
        return SPEC.isLoaded() ? ARS_MATRIX_SOURCE_JAR_RANGE.get() : 10;
    }

    public static boolean arsMatrixMeSource() {
        return !SPEC.isLoaded() || ARS_MATRIX_ME_SOURCE.get();
    }

    public static int arsMatrixMaxAccelerate() {
        return SPEC.isLoaded() ? ARS_MATRIX_MAX_ACCELERATE.get() : 16;
    }

    public static int arsMatrixAccelerateMinimumCraftTicks() {
        return SPEC.isLoaded() ? ARS_MATRIX_ACCELERATE_MINIMUM_CRAFT_TICKS.get() : 10;
    }

    public static int arsMatrixMaxDampen() {
        return SPEC.isLoaded() ? ARS_MATRIX_MAX_DAMPEN.get() : 16;
    }

    public static int arsMatrixDampenSourceDiscountPercent() {
        return SPEC.isLoaded() ? ARS_MATRIX_DAMPEN_SOURCE_DISCOUNT.get() : 50;
    }

    public static int arsMatrixMeDrain() {
        return SPEC.isLoaded() ? ARS_MATRIX_ME_DRAIN.get() : 500;
    }

    public static int arsMatrixIdleMeDrain() {
        return SPEC.isLoaded() ? ARS_MATRIX_IDLE_ME_DRAIN.get() : 10;
    }

    public static int arsMatrixChannels() {
        return SPEC.isLoaded() ? ARS_MATRIX_CHANNELS.get() : 15;
    }
}
