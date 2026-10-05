package dev.rylex.nep.data;

import dev.rylex.nep.Nep;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Nep.MOD_ID)
public final class NepDataGenerators {
    private NepDataGenerators() {}

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existing = event.getExistingFileHelper();
        generator.addProvider(event.includeClient(), new NepBlockStates(output, existing));
        generator.addProvider(event.includeClient(), new NepItemModels(output, existing));
    }
}
