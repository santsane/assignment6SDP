package com.inspection.visitor;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;

/**
 * One visit() overload per element type.
 *
 * Double dispatch: element.accept(visitor) picks the element's class at runtime,
 * and inside accept() the call visitor.visit(this) picks the right overload
 * because 'this' has a concrete type there. So two runtime choices
 * (which element + which visitor) happen without any instanceof.
 */
public interface ProjectVisitor {

    void visit(SourceCodeFile file);

    void visit(TestFile file);

    void visit(ConfigurationFile file);

    void visit(DependencyFile file);

    void visit(Directory directory);
}
