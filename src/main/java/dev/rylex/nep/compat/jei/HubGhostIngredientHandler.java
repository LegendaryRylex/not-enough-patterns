package dev.rylex.nep.compat.jei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.rylex.nep.client.GhostTarget;
import dev.rylex.nep.client.MachineHubScreen;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class HubGhostIngredientHandler implements IGhostIngredientHandler<MachineHubScreen> {

    @Override
    public <I> List<Target<I>> getTargetsTyped(
            MachineHubScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        AEKey key = keyOf(ingredient);
        if (key == null) {
            return List.of();
        }
        List<Target<I>> targets = new ArrayList<>();
        for (GhostTarget target : screen.ghostTargets()) {
            targets.add(new Target<>() {
                @Override
                public Rect2i getArea() {
                    return target.area();
                }

                @Override
                public void accept(I dragged) {
                    target.accept().accept(key);
                }
            });
        }
        return targets;
    }

    @Override
    public void onComplete() {}

    @Nullable
    private static AEKey keyOf(ITypedIngredient<?> ingredient) {
        ItemStack stack = ingredient.getIngredient(VanillaTypes.ITEM_STACK).orElse(null);
        if (stack != null) {
            GenericStack generic = GenericStack.fromItemStack(stack);
            return generic == null ? null : generic.what();
        }
        FluidStack fluid = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK).orElse(null);
        return fluid == null || fluid.isEmpty() ? null : AEFluidKey.of(fluid);
    }
}
