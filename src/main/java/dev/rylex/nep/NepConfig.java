package dev.rylex.nep;

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
    private static final ModConfigSpec.BooleanValue APOTHIC_OVERRIDE;
    private static final ModConfigSpec.BooleanValue APOTHIC_INFUSION;
    private static final ModConfigSpec.IntValue APOTHIC_INFUSION_EXPERIENCE_PER_BOTTLE;
    private static final ModConfigSpec.BooleanValue APOTHIC_INFUSION_PREFER_EXPERIENCE_FLUID;
    private static final ModConfigSpec.IntValue APOTHIC_INFUSION_MILLIBUCKETS_PER_EXPERIENCE;
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
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_CRAFT_TICKS.get() : 20;
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
