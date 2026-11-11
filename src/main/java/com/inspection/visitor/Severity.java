package com.inspection.visitor;

/** How bad a security issue is. penalty = how many points it takes off the score. */
public enum Severity {
    CRITICAL(15),
    HIGH(6),
    MEDIUM(3),
    LOW(1);

    private final int penalty;

    Severity(int penalty) {
        this.penalty = penalty;
    }

    public int getPenalty() {
        return penalty;
    }
}
