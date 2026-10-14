package dev.rylex.nep.machine;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

public final class CraftedOutputs {

    private static final int LIMIT = 256;

    private static final Map<AEItemKey, AEItemKey> DECLARED =
            Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<AEItemKey, AEItemKey> eldest) {
                    return size() > LIMIT;
                }
            });

    private CraftedOutputs() {}

    public static void expect(@Nullable AEItemKey actual, @Nullable AEItemKey declared) {
        if (actual != null && declared != null && !actual.equals(declared)) {
            DECLARED.put(actual, declared);
        }
    }

    @Nullable
    public static AEKey declaredFor(AEKey actual) {
        return DECLARED.isEmpty() || !(actual instanceof AEItemKey key) ? null : DECLARED.get(key);
    }

    public static void forget(@Nullable AEItemKey actual) {
        if (actual != null) {
            DECLARED.remove(actual);
        }
    }

    public static void clear() {
        DECLARED.clear();
    }
}
