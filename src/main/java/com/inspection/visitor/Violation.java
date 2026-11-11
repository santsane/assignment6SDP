package com.inspection.visitor;

/** One broken compliance rule. rule is a short id like NO_EXPOSED_CREDENTIALS. */
public record Violation(String rule, String element, String message) {

    @Override
    public String toString() {
        return "[" + rule + "] " + element + " - " + message;
    }
}
