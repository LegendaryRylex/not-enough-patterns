package dev.rylex.nep.hub;

import dev.rylex.nep.Nep;
import dev.rylex.nep.NepConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class HubRules {

    public static final TagKey<Block> PARTS = TagKey.create(Registries.BLOCK, Nep.id("machine_hub/parts"));
    public static final TagKey<Block> BLOCKED = TagKey.create(Registries.BLOCK, Nep.id("machine_hub/blocked"));
    public static final TagKey<Block> UNLINKABLE = TagKey.create(Registries.BLOCK, Nep.id("machine_hub/unlinkable"));

    private final Matcher whitelisted;
    private final Matcher blacklisted;
    private final Matcher linkBlacklisted;

    private HubRules(Matcher whitelisted, Matcher blacklisted, Matcher linkBlacklisted) {
        this.whitelisted = whitelisted;
        this.blacklisted = blacklisted;
        this.linkBlacklisted = linkBlacklisted;
    }

    public static HubRules load() {
        return new HubRules(
                Matcher.parse(NepConfig.machineHubScanWhitelist()),
                Matcher.parse(NepConfig.machineHubScanBlacklist()),
                Matcher.parse(NepConfig.machineHubLinkBlacklist()));
    }

    public boolean part(BlockState state) {
        return state.is(PARTS) || whitelisted.matches(state);
    }

    public boolean blocked(BlockState state) {
        return state.is(BLOCKED) || blacklisted.matches(state);
    }

    public boolean unlinkable(BlockState state) {
        return state.is(UNLINKABLE) || linkBlacklisted.matches(state);
    }

    public static boolean configEntry(Object entry) {
        if (!(entry instanceof String text)) {
            return false;
        }
        String candidate = text.endsWith(":*") ? text.substring(0, text.length() - 2) + ":x" : text;
        return ResourceLocation.tryParse(candidate) != null;
    }

    private record Matcher(Set<ResourceLocation> blocks, Set<String> mods) {

        private static Matcher parse(List<? extends String> entries) {
            Set<ResourceLocation> blocks = new HashSet<>();
            Set<String> mods = new HashSet<>();
            for (String entry : entries) {
                if (entry.endsWith(":*")) {
                    mods.add(entry.substring(0, entry.length() - 2));
                } else {
                    ResourceLocation id = ResourceLocation.tryParse(entry);
                    if (id != null) {
                        blocks.add(id);
                    }
                }
            }
            return new Matcher(blocks, mods);
        }

        private boolean matches(BlockState state) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            return mods.contains(id.getNamespace()) || blocks.contains(id);
        }
    }
}
