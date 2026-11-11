package com.inspection.element;

import com.inspection.visitor.ProjectVisitor;

import java.util.List;

/** A build file that lists dependencies (pom.xml, build.gradle...). */
public class DependencyFile implements ProjectElement {

    private final String name;
    private final List<Dependency> dependencies;

    public DependencyFile(String name, List<Dependency> dependencies) {
        this.name = name;
        this.dependencies = List.copyOf(dependencies);
    }

    public String getName() { return name; }
    public List<Dependency> getDependencies() { return dependencies; }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
