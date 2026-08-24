package dev.rylex.nep.guide;

import dev.rylex.nep.NepConfig;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import org.jetbrains.annotations.Nullable;

public final class NepConfigValues {

    private static final Map<String, LongSupplier> VALUES = values();

    private NepConfigValues() {}

    public static Set<String> names() {
        return VALUES.keySet();
    }

    @Nullable
    public static String format(String name) {
        LongSupplier value = VALUES.get(name);
        return value == null ? null : String.format(Locale.ROOT, "%,d", value.getAsLong());
    }

    private static Map<String, LongSupplier> values() {
        Map<String, LongSupplier> values = new LinkedHashMap<>();
        values.put("importCardGrace", NepConfig::importCardGrace);
        values.put("machineHubChannels", NepConfig::machineHubChannels);
        values.put("machineHubChannelsPerLink", NepConfig::machineHubChannelsPerLink);
        values.put("infusionExperiencePerBottle", NepConfig::apothicInfusionExperiencePerBottle);
        values.put("infusionMillibucketsPerExperience", NepConfig::apothicInfusionMillibucketsPerExperience);
        values.put("infusedAwakeningMatrixChannels", NepConfig::mysticalInfusedAwakeningMatrixChannels);
        values.put("infusedAwakeningMatrixTankCapacity", NepConfig::mysticalInfusedAwakeningMatrixTankCapacity);
        return Collections.unmodifiableMap(values);
    }
}
