package com.inspection.visitor;

/** One finding from the security audit. */
public record SecurityIssue(Severity severity, String element, String message) {

    @Override
    public String toString() {
        return "[" + severity + "] " + element + " - " + message;
    }
}
