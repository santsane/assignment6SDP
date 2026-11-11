package com.inspection.element;

import com.inspection.visitor.ProjectVisitor;

import java.util.LinkedHashMap;
import java.util.Map;

/** A config file like application.properties, stored as key/value settings. */
public class ConfigurationFile implements ProjectElement {

    private final String name;
    private final Map<String, String> settings;

    public ConfigurationFile(String name, Map<String, String> settings) {
        this.name = name;
        // LinkedHashMap copy so the report order stays the same as the input
        this.settings = new LinkedHashMap<>(settings);
    }

    public String getName() { return name; }
    public Map<String, String> getSettings() { return settings; }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
