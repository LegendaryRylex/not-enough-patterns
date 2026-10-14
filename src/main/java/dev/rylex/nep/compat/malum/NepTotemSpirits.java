package dev.rylex.nep.compat.malum;

/**
 * Spirit paths nep carves onto totem poles; Malum stores only the path in the blockstate, so these
 * names are reserved out of its namespace and mapped back to nep's on lookup.
 */
public final class NepTotemSpirits {
    private NepTotemSpirits() {}

    public static final String PURE_PATH = "pure";

    public static boolean isNepPath(String path) {
        return PURE_PATH.equals(path);
    }
}
