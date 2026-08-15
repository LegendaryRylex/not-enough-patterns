package dev.rylex.nep.hub;

import appeng.api.stacks.AEKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

public record HubFilter(List<@Nullable AEKey> keys, boolean allow) {

    public static final int SIZE = 9;

    public static final HubFilter EMPTY = new HubFilter(List.of(), true);

    public HubFilter {
        List<AEKey> padded = new ArrayList<>(SIZE);
        for (int slot = 0; slot < SIZE; slot++) {
            padded.add(slot < keys.size() ? keys.get(slot) : null);
        }
        keys = Collections.unmodifiableList(padded);
    }

    private record Slotted(int slot, AEKey key) {

        static final Codec<Slotted> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        Codec.intRange(0, SIZE - 1).fieldOf("slot").forGetter(Slotted::slot),
                        AEKey.CODEC.fieldOf("key").forGetter(Slotted::key))
                .apply(instance, Slotted::new));
    }

    public static final Codec<HubFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Slotted.CODEC.listOf().optionalFieldOf("keys", List.of()).forGetter(HubFilter::slotted),
                    Codec.BOOL.optionalFieldOf("allow", true).forGetter(HubFilter::allow))
            .apply(instance, HubFilter::fromSlotted));

    public static final StreamCodec<RegistryFriendlyByteBuf, HubFilter> STREAM_CODEC = StreamCodec.of(
            (buffer, filter) -> {
                for (AEKey key : filter.keys()) {
                    AEKey.OPTIONAL_STREAM_CODEC.encode(buffer, key);
                }
                buffer.writeBoolean(filter.allow());
            },
            buffer -> {
                List<AEKey> keys = new ArrayList<>(SIZE);
                for (int slot = 0; slot < SIZE; slot++) {
                    keys.add(AEKey.OPTIONAL_STREAM_CODEC.decode(buffer));
                }
                return new HubFilter(keys, buffer.readBoolean());
            });

    public boolean permits(AEKey what) {
        if (isEmpty()) {
            return true;
        }
        return keys.contains(what) == allow;
    }

    public boolean isEmpty() {
        for (AEKey key : keys) {
            if (key != null) {
                return false;
            }
        }
        return true;
    }

    public HubFilter withKey(int slot, @Nullable AEKey key) {
        if (slot < 0 || slot >= SIZE) {
            return this;
        }
        List<AEKey> updated = new ArrayList<>(keys);
        updated.set(slot, key);
        return new HubFilter(updated, allow);
    }

    public HubFilter toggled() {
        return new HubFilter(keys, !allow);
    }

    private List<Slotted> slotted() {
        List<Slotted> out = new ArrayList<>();
        for (int slot = 0; slot < SIZE; slot++) {
            AEKey key = keys.get(slot);
            if (key != null) {
                out.add(new Slotted(slot, key));
            }
        }
        return out;
    }

    private static HubFilter fromSlotted(List<Slotted> slotted, boolean allow) {
        List<AEKey> keys = new ArrayList<>(Collections.nCopies(SIZE, (AEKey) null));
        for (Slotted entry : slotted) {
            keys.set(entry.slot(), entry.key());
        }
        return new HubFilter(keys, allow);
    }
}
