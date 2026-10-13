package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.Nep;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.AcceleratorControllerBlockEntity;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;

final class AcceleratorProbe {
    private AcceleratorProbe() {}

    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;

    /** The live particle is private, and {@code getParticle()} falls back to the last one that left the ring. */
    @Nullable
    private static final VarHandle PARTICLE = particleHandle();

    static boolean available() {
        return PARTICLE != null;
    }

    @Nullable
    private static VarHandle particleHandle() {
        try {
            return MethodHandles.privateLookupIn(AcceleratorControllerBlockEntity.class, MethodHandles.lookup())
                    .findVarHandle(
                            AcceleratorControllerBlockEntity.class,
                            "particle",
                            AcceleratorParticleLogic.ActiveParticle.class);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            Nep.LOGGER.warn(
                    "Particle Collision Controller cannot see the accelerator's live particle, so it stays offline", e);
            return null;
        }
    }

    @Nullable
    static AcceleratorControllerBlockEntity at(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof AcceleratorControllerBlockEntity controller
                ? controller
                : null;
    }

    @Nullable
    static AcceleratorParticleLogic.ActiveParticle live(AcceleratorControllerBlockEntity controller) {
        return PARTICLE == null ? null : (AcceleratorParticleLogic.ActiveParticle) PARTICLE.get(controller);
    }

    static long speed(AcceleratorControllerBlockEntity controller) {
        AcceleratorParticleLogic.ActiveParticle particle = live(controller);
        return particle == null ? 0 : particle.velocity;
    }

    static boolean idle(AcceleratorControllerBlockEntity controller) {
        return live(controller) == null
                && controller.inventory.getItem(INPUT_SLOT).isEmpty()
                && controller.inventory.getItem(OUTPUT_SLOT).isEmpty();
    }

    static void load(AcceleratorControllerBlockEntity controller, ItemStack particle) {
        controller.inventory.setItem(INPUT_SLOT, particle);
    }

    static ItemStack unload(AcceleratorControllerBlockEntity controller) {
        ItemStack waiting = controller.inventory.getItem(INPUT_SLOT).copy();
        controller.inventory.setItem(INPUT_SLOT, ItemStack.EMPTY);
        return waiting;
    }

    static ItemStack takeResult(AcceleratorControllerBlockEntity controller) {
        ItemStack result = controller.inventory.getItem(OUTPUT_SLOT).copy();
        if (!result.isEmpty()) {
            controller.inventory.setItem(OUTPUT_SLOT, ItemStack.EMPTY);
        }
        return result;
    }

    /** A particle that flew out of the ring leaves its item in {@code activeItemParticle}; a collision clears it. */
    static boolean leftTheRing(AcceleratorControllerBlockEntity controller) {
        return live(controller) == null && !controller.activeItemParticle.isEmpty();
    }

    static ItemStack recall(AcceleratorControllerBlockEntity controller) {
        ItemStack item = controller.activeItemParticle.copy();
        controller.removeParticleDueToCollision();
        controller.setChanged();
        return item;
    }
}
