package dev.rylex.nep.mixin;

import java.util.List;

public abstract class ModGatedMixinPlugin extends BaseMixinPlugin {

    private final String targetMod;
    private final List<String> mixins;

    protected ModGatedMixinPlugin(String targetMod, String... mixins) {
        this.targetMod = targetMod;
        this.mixins = List.of(mixins);
    }

    @Override
    public List<String> getMixins() {
        return loaded(targetMod) ? mixins : List.of();
    }
}
