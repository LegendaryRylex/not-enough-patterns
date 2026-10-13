package dev.rylex.nep.compat.oritech;

import dev.rylex.nep.Nep;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.AcceleratorParticleLogic;
import rearth.oritech.block.entity.accelerator.ParticleAcceleratorBlockEntity;

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
            return MethodHandles.privateLookupIn(ParticleAcceleratorBlockEntity.class, MethodHandles.lookup())
                    .findVarHandle(
                            ParticleAcceleratorBlockEntity.class,
                            "particle",
                            AcceleratorParticleLogic.ActiveParticle.class);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            Nep.LOGGER.warn(
                    "Particle Collision Controller cannot see the accelerator's live particle, so it stays offline", e);
            return null;
        }
    }

    @Nullable
    static ParticleAcceleratorBlockEntity at(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ParticleAcceleratorBlockEntity controller
                ? controller
                : null;
    }

    @Nullable
    static AcceleratorParticleLogic.ActiveParticle live(ParticleAcceleratorBlockEntity controller) {
        return PARTICLE == null ? null : (AcceleratorParticleLogic.ActiveParticle) PARTICLE.get(controller);
    }

    static long speed(ParticleAcceleratorBlockEntity controller) {
        AcceleratorParticleLogic.ActiveParticle particle = live(controller);
        return particle == null ? 0 : particle.velocity;
    }

    static boolean idle(ParticleAcceleratorBlockEntity controller) {
        return live(controller) == null
                && controller.inventory.getItem(INPUT_SLOT).isEmpty()
                && controller.inventory.getItem(OUTPUT_SLOT).isEmpty();
    }

    static void load(ParticleAcceleratorBlockEntity controller, ItemStack particle) {
        controller.inventory.set(INPUT_SLOT, ItemResource.of(particle), particle.getCount());
    }

    static ItemStack unload(ParticleAcceleratorBlockEntity controller) {
        ItemStack waiting = controller.inventory.getItem(INPUT_SLOT).copy();
        controller.inventory.set(INPUT_SLOT, ItemResource.EMPTY, 0);
        return waiting;
    }

    static ItemStack takeResult(ParticleAcceleratorBlockEntity controller) {
        ItemStack result = controller.inventory.getItem(OUTPUT_SLOT).copy();
        if (!result.isEmpty()) {
            controller.inventory.set(OUTPUT_SLOT, ItemResource.EMPTY, 0);
        }
        return result;
    }

    /** A particle that flew out of the ring leaves its item in {@code activeItemParticle}; a collision clears it. */
    static boolean leftTheRing(ParticleAcceleratorBlockEntity controller) {
        return live(controller) == null && !controller.activeItemParticle.isEmpty();
    }

    static ItemStack recall(ParticleAcceleratorBlockEntity controller) {
        ItemStack item = controller.activeItemParticle.copy();
        controller.removeParticleDueToCollision();
        controller.setChanged();
        return item;
    }
}
