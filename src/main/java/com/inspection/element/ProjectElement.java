package com.inspection.element;

import com.inspection.visitor.ProjectVisitor;

/**
 * Anything that can live in a project (files, directories...).
 * The only thing an element has to know how to do is let a visitor in.
 */
public interface ProjectElement {

    void accept(ProjectVisitor visitor);
}
