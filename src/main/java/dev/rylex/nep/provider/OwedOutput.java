package dev.rylex.nep.provider;

public final class OwedOutput {

    public static final long NO_DEADLINE = -1;

    private long amount;
    private long deadline;

    public OwedOutput(long amount) {
        this(amount, NO_DEADLINE);
    }

    public OwedOutput(long amount, long deadline) {
        this.amount = amount;
        this.deadline = deadline;
    }

    public long amount() {
        return amount;
    }

    public void add(long more) {
        amount += more;
        deadline = NO_DEADLINE;
    }

    public boolean take(long imported) {
        amount -= imported;
        return amount <= 0;
    }

    public long deadline() {
        return deadline;
    }

    public boolean hasDeadline() {
        return deadline != NO_DEADLINE;
    }

    public void startGrace(long deadline) {
        this.deadline = deadline;
    }

    public void clearGrace() {
        deadline = NO_DEADLINE;
    }

    public boolean expired(long now) {
        return deadline != NO_DEADLINE && now >= deadline;
    }
}
