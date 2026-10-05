package dev.rylex.nep.compat.extendedae;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.GenericStack;
import com.glodblock.github.extendedae.container.pattern.ContainerPattern;
import com.glodblock.github.extendedae.container.pattern.PatternGuiHandler;
import dev.rylex.nep.Nep;
import dev.rylex.nep.pattern.NepPattern;
import dev.rylex.nep.pattern.RecipePattern;
import java.util.BitSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class PatternViewMenu extends ContainerPattern {

    public static final Identifier ID = Nep.id("pattern_view");
    public static final MenuType<PatternViewMenu> TYPE = PatternGuiHandler.register(ID, PatternViewMenu::new);

    static final int COLUMNS = 9;
    static final int INPUT_ROWS = 9;
    static final int OUTPUT_ROWS = 3;
    static final int INPUT_SLOTS = COLUMNS * INPUT_ROWS;

    private BitSet retained;

    public PatternViewMenu(@Nullable MenuType<?> menuType, int id, Level level, ItemStack stack) {
        super(menuType, level, id, stack);
        for (int row = 0; row < INPUT_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new DisplayOnlySlot(this, inputs, row * COLUMNS + col, 8 + col * 18, 9 + row * 18));
            }
        }
        for (int row = 0; row < OUTPUT_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new DisplayOnlySlot(this, outputs, row * COLUMNS + col, 8 + col * 18, 189 + row * 18));
            }
        }
    }

    boolean isRetained(@Nullable Slot slot) {
        return slot != null && retained != null && slot.index < INPUT_SLOTS && retained.get(slot.index);
    }

    @Override
    protected void analyse() {
        if (!(details instanceof NepPattern)) {
            invalidate();
            return;
        }
        layOutInputs();
        markRetained();
        for (GenericStack output : details.getOutputs()) {
            outputs.add(new GenericStack[] {output});
        }
    }

    private void layOutInputs() {
        for (IPatternDetails.IInput input : details.getInputs()) {
            GenericStack[] possible = input.getPossibleInputs();
            GenericStack[] scaled = new GenericStack[possible.length];
            for (int i = 0; i < possible.length; i++) {
                scaled[i] = new GenericStack(possible[i].what(), possible[i].amount() * input.getMultiplier());
            }
            inputs.add(scaled);
        }
    }

    private void markRetained() {
        if (!(details instanceof RecipePattern pattern) || pattern.retained().isEmpty()) {
            return;
        }
        retained = new BitSet(inputs.size());
        retained.set(inputs.size() - pattern.retained().size(), inputs.size());
    }
}
