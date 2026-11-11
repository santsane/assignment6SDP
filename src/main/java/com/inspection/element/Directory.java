package com.inspection.element;

import com.inspection.visitor.ProjectVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A folder. It holds other elements, including other directories,
 * so the whole project is a tree.
 *
 * Note: accept() only calls visitor.visit(this). Walking into the children
 * is the visitor's job (each visitor decides what to do with a directory).
 */
public class Directory implements ProjectElement {

    private final String name;
    private final List<ProjectElement> children = new ArrayList<>();

    public Directory(String name, ProjectElement... initialChildren) {
        this.name = name;
        Collections.addAll(children, initialChildren);
    }

    public Directory add(ProjectElement child) {
        children.add(child);
        return this;   // returning this so calls can be chained
    }

    public String getName() { return name; }

    public List<ProjectElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
