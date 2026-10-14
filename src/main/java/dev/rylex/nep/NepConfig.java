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
    private static final ModConfigSpec.IntValue MYSTICAL_MATRIX_CHANNELS;

    private static final ModConfigSpec.BooleanValue OCCULTISM_OVERRIDE;
    private static final ModConfigSpec.BooleanValue OCCULTISM_SPIRIT_PROVIDER;
    private static final ModConfigSpec.IntValue OCCULTISM_SPIRIT_PROVIDER_RANGE;
    private static final ModConfigSpec.IntValue OCCULTISM_SPIRIT_PROVIDER_QUEUE;
    private static final ModConfigSpec.BooleanValue OCCULTISM_MATRIX;
    private static final ModConfigSpec.BooleanValue OCCULTISM_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue OCCULTISM_MATRIX_SPIRIT_FIRE_TICKS;
    private static final ModConfigSpec.IntValue OCCULTISM_MATRIX_CRAFT_TIME;
    private static final ModConfigSpec.IntValue OCCULTISM_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue OCCULTISM_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue OCCULTISM_MATRIX_CHANNELS;
    private static final ModConfigSpec.BooleanValue RITUAL_CONTROLLER;
    private static final ModConfigSpec.BooleanValue RITUAL_CONTROLLER_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue RITUAL_CONTROLLER_BOWL_RANGE;
    private static final ModConfigSpec.IntValue RITUAL_CONTROLLER_TIER_SPEED;
    private static final ModConfigSpec.BooleanValue RITUAL_CONTROLLER_REPAIR;
    private static final ModConfigSpec.BooleanValue RITUAL_CONTROLLER_STAND_INS;
    private static final ModConfigSpec.IntValue RITUAL_CONTROLLER_ME_DRAIN;
    private static final ModConfigSpec.IntValue RITUAL_CONTROLLER_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue RITUAL_CONTROLLER_CHANNELS;
    private static final ModConfigSpec.BooleanValue ORITECH_OVERRIDE;
    private static final ModConfigSpec.BooleanValue ORITECH_PARTICLE_COLLISION;
    private static final ModConfigSpec.BooleanValue ORITECH_MATRIX;
    private static final ModConfigSpec.BooleanValue ORITECH_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue ORITECH_MATRIX_ENERGY_COST;
    private static final ModConfigSpec.IntValue ORITECH_MATRIX_CHARGE_RATE;
    private static final ModConfigSpec.IntValue ORITECH_MATRIX_CAPACITY;
    private static final ModConfigSpec.IntValue ORITECH_MATRIX_CRAFT_TICKS;
    private static final ModConfigSpec.BooleanValue ORITECH_MATRIX_ME_CHARGE;
    private static final ModConfigSpec.IntValue ORITECH_MATRIX_ME_DRAIN;
    private static final ModConfigSpec.IntValue ORITECH_MATRIX_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue ORITECH_MATRIX_CHANNELS;
    private static final ModConfigSpec.BooleanValue ORITECH_CONTROLLER;
    private static final ModConfigSpec.BooleanValue ORITECH_CONTROLLER_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue ORITECH_CONTROLLER_LINK_RANGE;
    private static final ModConfigSpec.IntValue ORITECH_CONTROLLER_COLLISION_TIMEOUT;
    private static final ModConfigSpec.IntValue ORITECH_CONTROLLER_ME_DRAIN;
    private static final ModConfigSpec.IntValue ORITECH_CONTROLLER_IDLE_ME_DRAIN;
    private static final ModConfigSpec.IntValue ORITECH_CONTROLLER_CHANNELS;

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
        MYSTICAL_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.mysticalagriculture.infusedAwakeningMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
        builder.pop();

        builder.pop();

        builder.comment("Occultism integration.")
                .translation("nep.configuration.modules.occultism")
                .push("occultism");
        OCCULTISM_OVERRIDE = builder.comment(
                        "Master override for Occultism integration.",
                        "When disabled, every Occultism module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.occultism.override")
                .define("allow_occultism_module", true);

        builder.comment("Bound Spirit Matrix:")
                .translation("nep.configuration.modules.occultism.boundSpiritMatrix")
                .push("boundSpiritMatrix");
        OCCULTISM_MATRIX = builder.comment(
                        "The Bound Spirit Matrix runs spirit fire, crushing and crystallizing recipes inside a single block.",
                        "Spirit fire always works; crushing and crystallizing need a Spirit-Infused Book of that job, whose spirit tier sets the recipes it can run, its speed and its output.")
                .translation("nep.configuration.modules.occultism.boundSpiritMatrix.enabled")
                .define("enabled", true);
        OCCULTISM_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Matrix left short of materials stalls until you manually restock it.")
                .translation("nep.configuration.modules.occultism.boundSpiritMatrix.autoRequest")
                .define("autoRequest", true);
        OCCULTISM_MATRIX_SPIRIT_FIRE_TICKS = builder.comment(
                        "Ticks the Matrix spends on a spirit fire craft. 20 ticks is one second.",
                        "Real spirit fire converts an item the moment it lands in the flames, so it has no per-item duration.")
                .translation("nep.configuration.modules.occultism.boundSpiritMatrix.spiritFireCraftTicks")
                .defineInRange("spiritFireCraftTicks", 20, 1, 24_000);
        OCCULTISM_MATRIX_CRAFT_TIME = builder.comment(
                        "Time a crushing or crystallizing craft takes, as a percentage of what the bound spirit would take in the world.",
                        "That base time is the recipe's own duration scaled by Occultism's per-tier time multiplier, so a stronger spirit is faster here too.")
                .translation("nep.configuration.modules.occultism.boundSpiritMatrix.craftTime")
                .defineInRange("craftTimePercent", 100, 1, 10_000);
        OCCULTISM_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while crafting, in AE per tick. Progress stops whenever the network cannot supply this much power.")
                .translation("nep.configuration.modules.occultism.boundSpiritMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        OCCULTISM_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.occultism.boundSpiritMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        OCCULTISM_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.occultism.boundSpiritMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
        builder.pop();

        builder.comment("Spirit Provider:")
                .translation("nep.configuration.modules.occultism.spiritProvider")
                .push("spiritProvider");
        OCCULTISM_SPIRIT_PROVIDER = builder.comment(
                        "The Spirit Provider takes crushing, crystallizing, smelting and trading crafts from a pattern provider against it and lays the inputs out for a working spirit to collect.",
                        "Only a spirit whose work area covers the Provider is served, and its finished products go back through the pattern provider instead of onto the ground.")
                .translation("nep.configuration.modules.occultism.spiritProvider.enabled")
                .define("enabled", true);
        OCCULTISM_SPIRIT_PROVIDER_RANGE = builder.comment(
                        "How far, in blocks along each axis, the Provider looks for working spirits.",
                        "A spirit further away than this is never served, even when its work area covers the Provider.")
                .translation("nep.configuration.modules.occultism.spiritProvider.searchRange")
                .defineInRange("searchRange", 16, 1, 64);
        OCCULTISM_SPIRIT_PROVIDER_QUEUE = builder.comment(
                        "Most crafts the Provider holds at once, counting those still waiting for a spirit.",
                        "A pattern provider is turned away while the Provider is full, and tries again later.")
                .translation("nep.configuration.modules.occultism.spiritProvider.queueLimit")
                .defineInRange("queueLimit", 8, 1, 256);
        builder.pop();

        builder.comment("Ritual Controller:")
                .translation("nep.configuration.modules.occultism.ritualController")
                .push("ritualController");
        RITUAL_CONTROLLER = builder.comment(
                        "The Ritual Controller runs Occultism crafting rituals from ritual patterns on a real pentacle: it lays the ingredients into the sacrificial bowls, starts the ritual at the golden bowl and collects the result.",
                        "It needs a Spirit-Infused Book, whose spirit tier sets the highest pentacle it may run and how fast its rituals go.")
                .translation("nep.configuration.modules.occultism.ritualController.enabled")
                .define("enabled", true);
        RITUAL_CONTROLLER_AUTO_REQUEST = builder.comment(
                        "Treats the Controller as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a ritual it owes is left without materials.",
                        "When disabled, a Controller left short of materials stalls until you restock it by hand.")
                .translation("nep.configuration.modules.occultism.ritualController.autoRequest")
                .define("autoRequest", true);
        RITUAL_CONTROLLER_BOWL_RANGE = builder.comment(
                        "How far, in blocks along each axis, the Controller looks for the golden sacrificial bowl it drives.",
                        "The nearest golden bowl in range is the one it uses.")
                .translation("nep.configuration.modules.occultism.ritualController.bowlRange")
                .defineInRange("bowlRange", 4, 1, 16);
        RITUAL_CONTROLLER_TIER_SPEED = builder.comment(
                        "Extra ritual speed for each spirit tier above foliot, as a percentage of the ritual's normal pace.",
                        "At 50, a djinni book runs rituals 1.5 times as fast, an afrit book twice as fast and a marid book 2.5 times as fast.")
                .translation("nep.configuration.modules.occultism.ritualController.tierSpeed")
                .defineInRange("tierSpeedPercent", 50, 0, 1000);
        RITUAL_CONTROLLER_REPAIR = builder.comment(
                        "Lets the Controller repair damaged items stored on its ME network with Occultism's repair rituals while it has no pattern work, once repairing is switched on in its screen.")
                .translation("nep.configuration.modules.occultism.ritualController.repair")
                .define("repair", true);
        RITUAL_CONTROLLER_STAND_INS = builder.comment(
                        "Lets the Controller satisfy a ritual's sacrifice with a stand-in item instead of a slain mob, such as a spawn egg or a soul gem holding the right creature.",
                        "Which items stand in for which mobs is set by nep:sacrifice_stand_in recipes.")
                .translation("nep.configuration.modules.occultism.ritualController.sacrificeStandIns")
                .define("sacrificeStandIns", true);
        RITUAL_CONTROLLER_ME_DRAIN = builder.comment(
                        "Energy the Controller draws from its ME network while a ritual runs, in AE per tick. A Controller that loses network power lets the ritual in progress finish, but starts no new one.")
                .translation("nep.configuration.modules.occultism.ritualController.meNetworkDrain")
                .defineInRange("meNetworkDrain", 100, 0, 1_000_000);
        RITUAL_CONTROLLER_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Controller draws from its ME network while idle, in AE per tick. This standing cost is read once when the Controller joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.occultism.ritualController.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        RITUAL_CONTROLLER_CHANNELS = builder.comment(
                        "Channels the Controller takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Controller joins a network, so changing it needs a world reload. Set to 0 for a Controller that takes up no channels at all.")
                .translation("nep.configuration.modules.occultism.ritualController.meNetworkChannels")
                .defineInRange("meNetworkChannels", 7, 0, 128);
        builder.pop();

        builder.pop();

        builder.comment("Oritech integration.")
                .translation("nep.configuration.modules.oritech")
                .push("oritech");
        ORITECH_OVERRIDE = builder.comment(
                        "Master override for Oritech integration.",
                        "When disabled, every Oritech module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.oritech.override")
                .define("allow_oritech_module", true);
        ORITECH_PARTICLE_COLLISION = builder.comment(
                        "Particle Collision Module: Encodes Particle Accelerator collision recipes as patterns, with the two colliding items as ingredients.",
                        "The patterns push their ingredients into adjacent inventories like a plain processing pattern.")
                .translation("nep.configuration.modules.oritech.particleCollision")
                .define("particleCollision", true);

        builder.comment("Particle Collision Matrix:")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix")
                .push("particleCollisionMatrix");
        ORITECH_MATRIX = builder.comment(
                        "The Particle Collision Matrix runs whole particle collision recipes inside a single block, with no accelerator ring.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.enabled")
                .define("enabled", true);
        ORITECH_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "When disabled, a Matrix left short of materials stalls until you manually restock it.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.autoRequest")
                .define("autoRequest", true);
        ORITECH_MATRIX_ENERGY_COST = builder.comment(
                        "Energy a Matrix craft costs, as a percentage of what Oritech's motors would spend speeding one particle up to the recipe's collision speed.",
                        "100 charges exactly what the real accelerator would; the cost grows with the square of the recipe's speed.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.energyCost")
                .defineInRange("energyCostPercent", 100, 0, 10_000);
        ORITECH_MATRIX_CHARGE_RATE = builder.comment("Maximum energy the Matrix accepts per tick, in FE.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.chargeRate")
                .defineInRange("chargeRate", 1_000_000, 1, Integer.MAX_VALUE);
        ORITECH_MATRIX_CAPACITY = builder.comment(
                        "Size of the Matrix's internal energy buffer, in FE.",
                        "A craft costing more than this still finishes, paying up to the buffer's worth each tick until it is covered.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.capacity")
                .defineInRange("energyCapacity", 10_000_000, 1_000, Integer.MAX_VALUE);
        ORITECH_MATRIX_CRAFT_TICKS = builder.comment(
                        "Fewest ticks a Matrix craft takes. 20 ticks is one second.",
                        "A craft lasts longer whenever its energy cannot be paid as fast as this allows.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.craftTicks")
                .defineInRange("craftTicks", 10, 1, 24_000);
        ORITECH_MATRIX_ME_CHARGE = builder.comment(
                        "Lets the Matrix charge a craft from its ME network's own power whenever its FE buffer runs short.",
                        "The network's last tenth of stored power is left alone, so a craft drawing its charge cannot brown the network out.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.meNetworkCharge")
                .define("meNetworkCharge", true);
        ORITECH_MATRIX_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while crafting, in AE per tick. Progress stops whenever the network cannot supply this much power.",
                        "This is separate from the FE it crafts with, and pays for the ME network side of the machine.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.meNetworkDrain")
                .defineInRange("meNetworkDrain", 500, 0, 1_000_000);
        ORITECH_MATRIX_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        ORITECH_MATRIX_CHANNELS = builder.comment(
                        "Channels the Matrix takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Matrix joins a network, so changing it needs a world reload. Set to 0 for a Matrix that takes up no channels at all.")
                .translation("nep.configuration.modules.oritech.particleCollisionMatrix.meNetworkChannels")
                .defineInRange("meNetworkChannels", 15, 0, 128);
        builder.pop();

        builder.comment("Particle Collision Controller:")
                .translation("nep.configuration.modules.oritech.particleCollisionController")
                .push("particleCollisionController");
        ORITECH_CONTROLLER = builder.comment(
                        "The Particle Collision Controller runs particle collision patterns on a real Particle Accelerator: it launches one particle, waits until it is fast enough, then releases the second into its path.",
                        "It links to two Particle Accelerators on the same ring with the Accelerator Linker, and only takes patterns whose speed it has seen a particle reach in that ring.")
                .translation("nep.configuration.modules.oritech.particleCollisionController.enabled")
                .define("enabled", true);
        ORITECH_CONTROLLER_AUTO_REQUEST = builder.comment(
                        "Treats the Controller as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a collision it owes is left without materials.",
                        "When disabled, a Controller left short of materials stalls until you restock it by hand.")
                .translation("nep.configuration.modules.oritech.particleCollisionController.autoRequest")
                .define("autoRequest", true);
        ORITECH_CONTROLLER_LINK_RANGE = builder.comment(
                        "Maximum distance, in blocks, that a linked Particle Accelerator may sit from the Particle Collision Controller.")
                .translation("nep.configuration.modules.oritech.particleCollisionController.linkRange")
                .defineInRange("linkRange", 64, 1, 256);
        ORITECH_CONTROLLER_COLLISION_TIMEOUT = builder.comment(
                        "Ticks the Controller waits for two launched particles to meet before it recalls both and gives the items back.",
                        "Raise it for a large ring where the fast particle needs a long lap to catch the slow one.")
                .translation("nep.configuration.modules.oritech.particleCollisionController.collisionTimeout")
                .defineInRange("collisionTimeout", 1_200, 20, 72_000);
        ORITECH_CONTROLLER_ME_DRAIN = builder.comment(
                        "Energy the Controller draws from its ME network while a particle is in flight, in AE per tick. A Controller that loses network power still finishes the collision in flight, but starts no new one.",
                        "The accelerator's motors still take their own RF, so this is only the Controller's share.")
                .translation("nep.configuration.modules.oritech.particleCollisionController.meNetworkDrain")
                .defineInRange("meNetworkDrain", 100, 0, 1_000_000);
        ORITECH_CONTROLLER_IDLE_ME_DRAIN = builder.comment(
                        "Energy the Controller draws from its ME network while idle, in AE per tick. This standing cost is read once when the Controller joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.oritech.particleCollisionController.idleMeNetworkDrain")
                .defineInRange("idleMeNetworkDrain", 10, 0, 1_000_000);
        ORITECH_CONTROLLER_CHANNELS = builder.comment(
                        "Channels the Controller takes up on its ME network. Any value above 8 has to pass through dense cables.",
                        "This is read once when the Controller joins a network, so changing it needs a world reload. Set to 0 for a Controller that takes up no channels at all.")
                .translation("nep.configuration.modules.oritech.particleCollisionController.meNetworkChannels")
                .defineInRange("meNetworkChannels", 7, 0, 128);
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

    private static int clampedIdle(ModConfigSpec.IntValue idle, int active) {
        return Math.min(SPEC.isLoaded() ? idle.get() : idle.getDefault(), active);
    }

    private static void warnIdleAboveActive(
            String machine, ModConfigSpec.IntValue idle, ModConfigSpec.IntValue active) {
        int idleDrain = idle.get();
        int activeDrain = active.get();
        if (idleDrain > activeDrain) {
            Nep.LOGGER.warn(
                    "{} idleMeNetworkDrain ({} AE/t) is above meNetworkDrain ({} AE/t); the idle"
                            + " drain is clamped to {} AE/t, so working costs no more than sitting idle.",
                    machine,
                    idleDrain,
                    activeDrain,
                    activeDrain);
        }
    }

    public static void onConfigLoaded(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        NepConfigGates.changed();
        warnIdleAboveActive("Infused Awakening Matrix", MYSTICAL_MATRIX_IDLE_ME_DRAIN, MYSTICAL_MATRIX_ME_DRAIN);
        warnIdleAboveActive("Occultism Matrix", OCCULTISM_MATRIX_IDLE_ME_DRAIN, OCCULTISM_MATRIX_ME_DRAIN);
        warnIdleAboveActive("Ritual Controller", RITUAL_CONTROLLER_IDLE_ME_DRAIN, RITUAL_CONTROLLER_ME_DRAIN);
        warnIdleAboveActive("Oritech Matrix", ORITECH_MATRIX_IDLE_ME_DRAIN, ORITECH_MATRIX_ME_DRAIN);
        warnIdleAboveActive("Oritech Controller", ORITECH_CONTROLLER_IDLE_ME_DRAIN, ORITECH_CONTROLLER_ME_DRAIN);
    }

    public static boolean loaded() {
        return SPEC.isLoaded();
    }

    public static int importCardGrace() {
        return SPEC.isLoaded() ? IMPORT_CARD_GRACE.get() : IMPORT_CARD_GRACE.getDefault();
    }

    public static boolean debugLogging() {
        return SPEC.isLoaded() && DEBUG_LOGGING.get();
    }

    public static boolean manualCrafting() {
        return !SPEC.isLoaded() || MANUAL_CRAFTING.get();
    }

    public static boolean processingPatternConversion() {
        return SPEC.isLoaded() ? PROCESSING_PATTERN_CONVERSION.get() : PROCESSING_PATTERN_CONVERSION.getDefault();
    }

    public static boolean requireDecoder() {
        return SPEC.isLoaded() && REQUIRE_DECODER.get();
    }

    public static boolean decoderRequiredFor(DecoderModule module) {
        return requireDecoder() && DECODER_MODULES.get(module).get();
    }

    public static int patternDecoderChannels() {
        return SPEC.isLoaded() ? PATTERN_DECODER_CHANNELS.get() : PATTERN_DECODER_CHANNELS.getDefault();
    }

    public static int patternDecoderIdleMeDrain() {
        return SPEC.isLoaded() ? PATTERN_DECODER_IDLE_ME_DRAIN.get() : PATTERN_DECODER_IDLE_ME_DRAIN.getDefault();
    }

    public static boolean machineHubEnabled() {
        return !SPEC.isLoaded() || MACHINE_HUB.get();
    }

    public static int machineHubLinkRange() {
        return SPEC.isLoaded() ? MACHINE_HUB_LINK_RANGE.get() : MACHINE_HUB_LINK_RANGE.getDefault();
    }

    public static int machineHubMaximumLinks() {
        return SPEC.isLoaded() ? MACHINE_HUB_MAXIMUM_LINKS.get() : MACHINE_HUB_MAXIMUM_LINKS.getDefault();
    }

    public static int machineHubScanBudget() {
        return SPEC.isLoaded() ? MACHINE_HUB_SCAN_BUDGET.get() : MACHINE_HUB_SCAN_BUDGET.getDefault();
    }

    public static int machineHubCasingDepth() {
        return SPEC.isLoaded() ? MACHINE_HUB_CASING_DEPTH.get() : MACHINE_HUB_CASING_DEPTH.getDefault();
    }

    public static List<? extends String> machineHubScanWhitelist() {
        return SPEC.isLoaded()
                ? MACHINE_HUB_SCAN_WHITELIST.get()
                : List.copyOf(MACHINE_HUB_SCAN_WHITELIST.getDefault());
    }

    public static List<? extends String> machineHubScanBlacklist() {
        return SPEC.isLoaded()
                ? MACHINE_HUB_SCAN_BLACKLIST.get()
                : List.copyOf(MACHINE_HUB_SCAN_BLACKLIST.getDefault());
    }

    public static List<? extends String> machineHubLinkBlacklist() {
        return SPEC.isLoaded()
                ? MACHINE_HUB_LINK_BLACKLIST.get()
                : List.copyOf(MACHINE_HUB_LINK_BLACKLIST.getDefault());
    }

    public static int machineHubChannels() {
        return SPEC.isLoaded() ? MACHINE_HUB_CHANNELS.get() : MACHINE_HUB_CHANNELS.getDefault();
    }

    public static int machineHubChannelsPerLink() {
        return SPEC.isLoaded() ? MACHINE_HUB_CHANNELS_PER_LINK.get() : MACHINE_HUB_CHANNELS_PER_LINK.getDefault();
    }

    public static boolean apothicOverride() {
        return SPEC.isLoaded() && APOTHIC_OVERRIDE.get();
    }

    public static boolean apothicInfusion() {
        return apothicOverride() && APOTHIC_INFUSION.get();
    }

    public static int apothicInfusionExperiencePerBottle() {
        return SPEC.isLoaded()
                ? APOTHIC_INFUSION_EXPERIENCE_PER_BOTTLE.get()
                : APOTHIC_INFUSION_EXPERIENCE_PER_BOTTLE.getDefault();
    }

    public static boolean apothicInfusionPreferExperienceFluid() {
        return !SPEC.isLoaded() || APOTHIC_INFUSION_PREFER_EXPERIENCE_FLUID.get();
    }

    public static int apothicInfusionMillibucketsPerExperience() {
        return SPEC.isLoaded()
                ? APOTHIC_INFUSION_MILLIBUCKETS_PER_EXPERIENCE.get()
                : APOTHIC_INFUSION_MILLIBUCKETS_PER_EXPERIENCE.getDefault();
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
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_CRAFT_TICKS.get() : MYSTICAL_MATRIX_CRAFT_TICKS.getDefault();
    }

    public static long mysticalInfusedAwakeningMatrixTankCapacity() {
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_TANK_CAPACITY.get() : MYSTICAL_MATRIX_TANK_CAPACITY.getDefault();
    }

    public static int mysticalInfusedAwakeningMatrixMeDrain() {
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_ME_DRAIN.get() : MYSTICAL_MATRIX_ME_DRAIN.getDefault();
    }

    public static int mysticalInfusedAwakeningMatrixIdleMeDrain() {
        return clampedIdle(MYSTICAL_MATRIX_IDLE_ME_DRAIN, mysticalInfusedAwakeningMatrixMeDrain());
    }

    public static int mysticalInfusedAwakeningMatrixChannels() {
        return SPEC.isLoaded() ? MYSTICAL_MATRIX_CHANNELS.get() : MYSTICAL_MATRIX_CHANNELS.getDefault();
    }

    public static boolean occultismOverride() {
        return SPEC.isLoaded() && OCCULTISM_OVERRIDE.get();
    }

    public static boolean occultismMatrix() {
        return occultismOverride() && OCCULTISM_MATRIX.get();
    }

    public static boolean occultismMatrixAutoRequest() {
        return occultismMatrix() && OCCULTISM_MATRIX_AUTO_REQUEST.get();
    }

    public static int occultismMatrixSpiritFireTicks() {
        return SPEC.isLoaded()
                ? OCCULTISM_MATRIX_SPIRIT_FIRE_TICKS.get()
                : OCCULTISM_MATRIX_SPIRIT_FIRE_TICKS.getDefault();
    }

    public static int occultismMatrixCraftTimePercent() {
        return SPEC.isLoaded() ? OCCULTISM_MATRIX_CRAFT_TIME.get() : OCCULTISM_MATRIX_CRAFT_TIME.getDefault();
    }

    public static int occultismMatrixMeDrain() {
        return SPEC.isLoaded() ? OCCULTISM_MATRIX_ME_DRAIN.get() : OCCULTISM_MATRIX_ME_DRAIN.getDefault();
    }

    public static int occultismMatrixIdleMeDrain() {
        return clampedIdle(OCCULTISM_MATRIX_IDLE_ME_DRAIN, occultismMatrixMeDrain());
    }

    public static int occultismMatrixChannels() {
        return SPEC.isLoaded() ? OCCULTISM_MATRIX_CHANNELS.get() : OCCULTISM_MATRIX_CHANNELS.getDefault();
    }

    public static boolean occultismSpiritProvider() {
        return occultismOverride() && OCCULTISM_SPIRIT_PROVIDER.get();
    }

    public static int occultismSpiritProviderRange() {
        return SPEC.isLoaded() ? OCCULTISM_SPIRIT_PROVIDER_RANGE.get() : OCCULTISM_SPIRIT_PROVIDER_RANGE.getDefault();
    }

    public static int occultismSpiritProviderQueue() {
        return SPEC.isLoaded() ? OCCULTISM_SPIRIT_PROVIDER_QUEUE.get() : OCCULTISM_SPIRIT_PROVIDER_QUEUE.getDefault();
    }

    public static boolean occultismRitualController() {
        return occultismOverride() && RITUAL_CONTROLLER.get();
    }

    public static boolean occultismRitualControllerAutoRequest() {
        return occultismRitualController() && RITUAL_CONTROLLER_AUTO_REQUEST.get();
    }

    public static int occultismRitualControllerBowlRange() {
        return SPEC.isLoaded() ? RITUAL_CONTROLLER_BOWL_RANGE.get() : RITUAL_CONTROLLER_BOWL_RANGE.getDefault();
    }

    public static int occultismRitualControllerTierSpeed() {
        return SPEC.isLoaded() ? RITUAL_CONTROLLER_TIER_SPEED.get() : RITUAL_CONTROLLER_TIER_SPEED.getDefault();
    }

    public static boolean occultismRitualControllerRepair() {
        return occultismRitualController() && RITUAL_CONTROLLER_REPAIR.get();
    }

    public static boolean occultismSacrificeStandIns() {
        return occultismRitualController() && RITUAL_CONTROLLER_STAND_INS.get();
    }

    public static int occultismRitualControllerMeDrain() {
        return SPEC.isLoaded() ? RITUAL_CONTROLLER_ME_DRAIN.get() : RITUAL_CONTROLLER_ME_DRAIN.getDefault();
    }

    public static int occultismRitualControllerIdleMeDrain() {
        return clampedIdle(RITUAL_CONTROLLER_IDLE_ME_DRAIN, occultismRitualControllerMeDrain());
    }

    public static int occultismRitualControllerChannels() {
        return SPEC.isLoaded() ? RITUAL_CONTROLLER_CHANNELS.get() : RITUAL_CONTROLLER_CHANNELS.getDefault();
    }

    public static boolean oritechOverride() {
        return SPEC.isLoaded() && ORITECH_OVERRIDE.get();
    }

    public static boolean oritechParticleCollision() {
        return oritechOverride() && ORITECH_PARTICLE_COLLISION.get();
    }

    public static boolean oritechParticleCollisionMatrix() {
        return oritechOverride() && ORITECH_MATRIX.get();
    }

    public static boolean oritechMatrixAutoRequest() {
        return oritechParticleCollisionMatrix() && ORITECH_MATRIX_AUTO_REQUEST.get();
    }

    public static long oritechMatrixEnergyCostPercent() {
        return SPEC.isLoaded() ? ORITECH_MATRIX_ENERGY_COST.get() : ORITECH_MATRIX_ENERGY_COST.getDefault();
    }

    public static int oritechMatrixChargeRate() {
        return SPEC.isLoaded() ? ORITECH_MATRIX_CHARGE_RATE.get() : ORITECH_MATRIX_CHARGE_RATE.getDefault();
    }

    public static int oritechMatrixCapacity() {
        return SPEC.isLoaded() ? ORITECH_MATRIX_CAPACITY.get() : ORITECH_MATRIX_CAPACITY.getDefault();
    }

    public static int oritechMatrixCraftTicks() {
        return SPEC.isLoaded() ? ORITECH_MATRIX_CRAFT_TICKS.get() : ORITECH_MATRIX_CRAFT_TICKS.getDefault();
    }

    public static boolean oritechMatrixMeCharge() {
        return !SPEC.isLoaded() || ORITECH_MATRIX_ME_CHARGE.get();
    }

    public static int oritechMatrixMeDrain() {
        return SPEC.isLoaded() ? ORITECH_MATRIX_ME_DRAIN.get() : ORITECH_MATRIX_ME_DRAIN.getDefault();
    }

    public static int oritechMatrixIdleMeDrain() {
        return clampedIdle(ORITECH_MATRIX_IDLE_ME_DRAIN, oritechMatrixMeDrain());
    }

    public static int oritechMatrixChannels() {
        return SPEC.isLoaded() ? ORITECH_MATRIX_CHANNELS.get() : ORITECH_MATRIX_CHANNELS.getDefault();
    }

    public static boolean oritechParticleCollisionController() {
        return oritechOverride() && ORITECH_CONTROLLER.get();
    }

    public static boolean oritechControllerAutoRequest() {
        return oritechParticleCollisionController() && ORITECH_CONTROLLER_AUTO_REQUEST.get();
    }

    public static int oritechControllerLinkRange() {
        return SPEC.isLoaded() ? ORITECH_CONTROLLER_LINK_RANGE.get() : ORITECH_CONTROLLER_LINK_RANGE.getDefault();
    }

    public static int oritechControllerCollisionTimeout() {
        return SPEC.isLoaded()
                ? ORITECH_CONTROLLER_COLLISION_TIMEOUT.get()
                : ORITECH_CONTROLLER_COLLISION_TIMEOUT.getDefault();
    }

    public static int oritechControllerMeDrain() {
        return SPEC.isLoaded() ? ORITECH_CONTROLLER_ME_DRAIN.get() : ORITECH_CONTROLLER_ME_DRAIN.getDefault();
    }

    public static int oritechControllerIdleMeDrain() {
        return clampedIdle(ORITECH_CONTROLLER_IDLE_ME_DRAIN, oritechControllerMeDrain());
    }

    public static int oritechControllerChannels() {
        return SPEC.isLoaded() ? ORITECH_CONTROLLER_CHANNELS.get() : ORITECH_CONTROLLER_CHANNELS.getDefault();
    }
}
