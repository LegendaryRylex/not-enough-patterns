package dev.rylex.nep.compat.provider.mixin;

import dev.rylex.nep.mixin.BaseMixinPlugin;
import java.util.ArrayList;
import java.util.List;

public class ProviderMixinPlugin extends BaseMixinPlugin {

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();
        if (loaded("appliedcreate")) {
            mixins.add("MechanicalCraftingPatternLogicMixin");
        }
        if (loaded("ae2_draconic_fusion_autocrafter")) {
            mixins.add("DraconicPatternProviderLogicMixin");
        }
        return mixins;
    }
}
