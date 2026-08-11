package dev.rylex.nep;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

public final class NepTestInstance extends GameTestInstance {

    public static final MapCodec<NepTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("name").forGetter(NepTestInstance::name),
                    TestData.CODEC.forGetter(NepTestInstance::data))
            .apply(instance, NepTestInstance::new));

    private final Identifier name;

    public NepTestInstance(Identifier name, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.name = name;
    }

    @Override
    public void run(GameTestHelper helper) {
        NepGameTests.body(name).accept(helper);
    }

    @Override
    public MapCodec<NepTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("nep");
    }

    private Identifier name() {
        return name;
    }

    private TestData<Holder<TestEnvironmentDefinition<?>>> data() {
        return info();
    }
}
