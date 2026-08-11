package dev.rylex.nep.machine;

public record PushOutcome(boolean accepted, boolean unsatisfiable, String detail) {

    public static PushOutcome accepted(String detail) {
        return new PushOutcome(true, false, detail);
    }

    public static PushOutcome retry(String reason) {
        return new PushOutcome(false, false, reason);
    }

    public static PushOutcome unsatisfiable(String reason) {
        return new PushOutcome(false, true, reason);
    }
}
