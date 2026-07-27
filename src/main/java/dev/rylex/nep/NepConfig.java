package dev.rylex.nep;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class NepConfig {
    private NepConfig() {}

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue DEBUG_LOGGING;
    private static final ModConfigSpec.BooleanValue CREATE_OVERRIDE;
    private static final ModConfigSpec.BooleanValue CREATE_MECHANICAL_CRAFTING;
    private static final ModConfigSpec.BooleanValue CREATE_MECHANICAL_CRAFTING_ALLOW_REGULAR;
    private static final ModConfigSpec.BooleanValue CREATE_DEPLOYING;
    private static final ModConfigSpec.BooleanValue CREATE_FILLING;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_LINK_RANGE;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_TANK_CAPACITY;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_HALT_GRACE;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_MATRIX;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_GUARANTEED_RESULTS;
    private static final ModConfigSpec.BooleanValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_AUTO_REQUEST;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_POWER;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_IDLE_POWER;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS_MINIMUM;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_MINIMUM_SPEED;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_CRAFT_TICKS;
    private static final ModConfigSpec.IntValue CREATE_SEQUENCED_ASSEMBLY_MATRIX_TANK_CAPACITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                        "Per-mod integration modules with individual per-machine integration toggles.",
                        "Disabled integrations stop encoding patterns and stop pattern providers from pushing to those machines; already-encoded patterns stay in the world and resume working when the integration is re-enabled.")
                .translation("nep.configuration.modules")
                .push("modules");

        builder.comment("Create integration.")
                .translation("nep.configuration.modules.create")
                .push("create");
        CREATE_OVERRIDE = builder.comment(
                        "Master override for Create integration.",
                        "When disabled, every Create module below is disabled regardless of its own toggle.")
                .translation("nep.configuration.modules.create.override")
                .define("allow_create_module", true);

        builder.comment("Mechanical Crafter Module:")
                .translation("nep.configuration.modules.create.mechanicalCrafting")
                .push("mechanicalCrafting");
        CREATE_MECHANICAL_CRAFTING = builder.translation("nep.configuration.modules.create.mechanicalCrafting.enabled")
                .define("enabled", true);
        CREATE_MECHANICAL_CRAFTING_ALLOW_REGULAR = builder.comment(
                        "Toggles crafting/processing patterns auto-resolving to vanilla shaped or shapeless recipes for Mechanical Crafters.",
                        "When disabled, only genuine Create Mechanical Crafting recipes are driven.",
                        "Also requires Create's own 'recipes.allowRegularCraftingInCrafter' to be enabled.")
                .translation("nep.configuration.modules.create.mechanicalCrafting.allowRegularCrafting")
                .define("allowRegularCrafting", true);
        builder.pop();

        builder.comment("Deployer Module:")
                .translation("nep.configuration.modules.create.deploying")
                .push("deploying");
        CREATE_DEPLOYING = builder.translation("nep.configuration.modules.create.deploying.enabled")
                .define("enabled", true);
        builder.pop();

        builder.comment("Spout Module:")
                .translation("nep.configuration.modules.create.filling")
                .push("filling");
        CREATE_FILLING = builder.translation("nep.configuration.modules.create.filling.enabled")
                .define("enabled", true);
        builder.pop();

        builder.comment("Sequenced Assembly Controller:")
                .translation("nep.configuration.modules.create.sequencedAssembly")
                .push("sequencedAssembly");
        CREATE_SEQUENCED_ASSEMBLY = builder.translation("nep.configuration.modules.create.sequencedAssembly.enabled")
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
        builder.pop();

        builder.comment("Sequenced Assembly Matrix:")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix")
                .push("sequencedAssemblyMatrix");
        CREATE_SEQUENCED_ASSEMBLY_MATRIX = builder.comment(
                        "The Sequenced Assembly Matrix runs a whole sequenced assembly recipe inside a single block.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.enabled")
                .define("enabled", true);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_GUARANTEED_RESULTS = builder.comment(
                        "Bypasses the output chances of sequenced assembly recipes, so every Matrix craft yields the recipe's primary result.",
                        "When disabled, the Matrix rolls the recipe's result pool exactly as a physical line does: unlucky rolls produce junk, which is placed into network storage, and the ingredients for that attempt are lost.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.guaranteedResults")
                .define("guaranteedResults", true);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_AUTO_REQUEST = builder.comment(
                        "Treats the Sequenced Assembly Matrix as a requester, allowing it to auto-pull, auto-craft, and auto-request a set of recipe ingredients from the ME network when a craft it owes is left without materials.",
                        "This is what lets a Matrix with guaranteedResults disabled retry after a chance recipe rolls junk. When disabled, an unlucky streak stalls the Matrix until you restock it by hand.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.autoRequest")
                .define("autoRequest", true);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_POWER = builder.comment(
                        "Energy the Matrix draws from its ME network while assembling, in AE per tick. Progress stops whenever the network cannot supply this much power.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.powerUsage")
                .defineInRange("powerUsage", 500, 0, 1_000_000);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_IDLE_POWER = builder.comment(
                        "Energy the Matrix draws from its ME network while idle, in AE per tick. This standing cost is read once when the Matrix joins a network, so changing it needs a world reload.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.idlePowerUsage")
                .defineInRange("idlePowerUsage", 10, 0, 1_000_000);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS = builder.comment(
                        "Rotational stress the Matrix draws once it reaches Create's maximum rotation speed, in Create Stress Units.",
                        "Draw rises with shaft speed between minimumSpeed and that maximum, and a Matrix drawing more stress assembles faster.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.stressUnits")
                .defineInRange("stressUnits", 294_912, 1, 16_777_216);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS_MINIMUM = builder.comment(
                        "Rotational stress the Matrix draws at minimumSpeed (see below), in Create Stress Units. This is the least stress a working Matrix can cost.",
                        "The ratio between stressUnits and this figure is how fast a fully fed Matrix assembles.")
                .translation("nep.configuration.modules.create.sequencedAssemblyMatrix.stressUnitsMinimum")
                .defineInRange("stressUnitsMinimum", 8_192, 1, 16_777_216);
        CREATE_SEQUENCED_ASSEMBLY_MATRIX_MINIMUM_SPEED = builder.comment(
                        "Shaft speed the Matrix needs to run at all, in RPM.",
                        "Below this the Matrix does no work and draws no stress at all.")
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
        builder.pop();

        builder.pop();

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
        int idlePower = CREATE_SEQUENCED_ASSEMBLY_MATRIX_IDLE_POWER.get();
        int activePower = CREATE_SEQUENCED_ASSEMBLY_MATRIX_POWER.get();
        if (idlePower > activePower) {
            Nep.LOGGER.warn(
                    "Sequenced Assembly Matrix idlePowerUsage ({} AE/t) is above powerUsage ({} AE/t); the idle draw is"
                            + " clamped to {} AE/t, so assembling costs no more than sitting idle.",
                    idlePower,
                    activePower,
                    activePower);
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

    public static boolean debugLogging() {
        return SPEC.isLoaded() && DEBUG_LOGGING.get();
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

    public static boolean createSequencedAssemblyMatrix() {
        return createOverride() && CREATE_SEQUENCED_ASSEMBLY_MATRIX.get();
    }

    public static boolean createSequencedAssemblyMatrixGuaranteedResults() {
        return !SPEC.isLoaded() || CREATE_SEQUENCED_ASSEMBLY_MATRIX_GUARANTEED_RESULTS.get();
    }

    public static boolean createSequencedAssemblyMatrixAutoRequest() {
        return createSequencedAssemblyMatrix() && CREATE_SEQUENCED_ASSEMBLY_MATRIX_AUTO_REQUEST.get();
    }

    public static int createSequencedAssemblyMatrixPower() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_POWER.get() : 500;
    }

    public static int createSequencedAssemblyMatrixStress() {
        return SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_STRESS.get() : 294_912;
    }

    public static int createSequencedAssemblyMatrixIdlePower() {
        int idle = SPEC.isLoaded() ? CREATE_SEQUENCED_ASSEMBLY_MATRIX_IDLE_POWER.get() : 10;
        return Math.min(idle, createSequencedAssemblyMatrixPower());
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
}
