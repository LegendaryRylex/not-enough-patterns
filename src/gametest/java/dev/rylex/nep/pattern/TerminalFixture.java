package dev.rylex.nep.pattern;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.parts.encoding.PatternEncodingLogic;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

final class TerminalFixture implements IPatternTerminalLogicHost {

    private final Level level;
    private final PatternEncodingLogic logic;

    static GenericStack stack(Item item, long amount) {
        return new GenericStack(AEItemKey.of(item), amount);
    }

    static ItemStack obsidianToDiamond(long obsidian) {
        return PatternDetailsHelper.encodeProcessingPattern(
                List.of(stack(Items.OBSIDIAN, obsidian)), List.of(stack(Items.DIAMOND, 1)));
    }

    TerminalFixture(Level level) {
        this.level = level;
        this.logic = new PatternEncodingLogic(this);
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return logic;
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public void markForSave() {}
}
