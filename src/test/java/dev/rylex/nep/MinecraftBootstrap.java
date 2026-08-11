package dev.rylex.nep;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class MinecraftBootstrap implements BeforeAllCallback {

    private static boolean done;

    @Nullable
    private static HolderLookup.Provider registries;

    public MinecraftBootstrap() {}

    @Override
    public void beforeAll(ExtensionContext context) {
        ensure();
    }

    public static synchronized HolderLookup.Provider registries() {
        ensure();
        return registries;
    }

    static synchronized void ensure() {
        if (done) {
            return;
        }
        done = true;
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        boolean validating = SharedConstants.IS_RUNNING_IN_IDE;
        SharedConstants.IS_RUNNING_IN_IDE = false;
        try {
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                    .build(untagged(registries))
                    .forEach(DataComponentInitializers.PendingComponents::apply);
        } finally {
            SharedConstants.IS_RUNNING_IN_IDE = validating;
        }
    }

    private static HolderLookup.Provider untagged(HolderLookup.Provider delegate) {
        return new HolderLookup.Provider() {
            @Override
            public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                return delegate.listRegistryKeys();
            }

            @Override
            public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(
                    ResourceKey<? extends Registry<? extends T>> key) {
                Optional<? extends HolderLookup.RegistryLookup<T>> found = delegate.lookup(key);
                return Optional.of(found.<HolderLookup.RegistryLookup<T>>map(MinecraftBootstrap::untagged)
                        .orElseGet(() -> absent(key)));
            }
        };
    }

    @SuppressWarnings("deprecation")
    private static <T> HolderLookup.RegistryLookup<T> untagged(HolderLookup.RegistryLookup<T> delegate) {
        return new HolderLookup.RegistryLookup.Delegate<T>() {
            @Override
            public HolderLookup.RegistryLookup<T> parent() {
                return delegate;
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
                return Optional.of(delegate.get(id).orElseGet(() -> Holder.Reference.createStandAlone(this, id)));
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                return Optional.of(delegate.get(id).orElseGet(() -> HolderSet.emptyNamed(this, id)));
            }
        };
    }

    @SuppressWarnings("deprecation")
    private static <T> HolderLookup.RegistryLookup<T> absent(ResourceKey<? extends Registry<? extends T>> key) {
        return new HolderLookup.RegistryLookup<T>() {
            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return key;
            }

            @Override
            public Lifecycle registryLifecycle() {
                return Lifecycle.experimental();
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> id) {
                return Optional.of(Holder.Reference.createStandAlone(this, id));
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return Stream.empty();
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> id) {
                return Optional.of(HolderSet.emptyNamed(this, id));
            }

            @Override
            public Stream<HolderSet.Named<T>> listTags() {
                return Stream.empty();
            }
        };
    }
}
