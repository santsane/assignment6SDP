package com.inspection.element;

/**
 * One dependency line from a pom.xml / build file.
 * version can be null or blank if the developer forgot to write it.
 */
public record Dependency(String name, String version) {

    public boolean hasVersion() {
        return version != null && !version.isBlank();
    }

    @Override
    public String toString() {
        return hasVersion() ? name + ":" + version : name + " (no version)";
    }
}
