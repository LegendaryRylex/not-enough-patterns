package dev.rylex.nep.decoder;

public enum DecoderStatus {
    NO_NETWORK("gui.nep.pattern_decoder.status.no_network"),
    NO_CHANNEL("gui.nep.pattern_decoder.status.no_channel"),
    OFFLINE("gui.nep.pattern_decoder.status.offline"),
    ONLINE("gui.nep.pattern_decoder.status.online");

    private final String key;

    DecoderStatus(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static DecoderStatus byOrdinal(int ordinal) {
        DecoderStatus[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NO_NETWORK;
    }
}
