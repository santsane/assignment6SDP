package com.inspection.element;

import com.inspection.visitor.ProjectVisitor;

import java.util.List;

/** A production .java file. Just data, no inspection logic here. */
public class SourceCodeFile implements ProjectElement {

    private final String name;
    private final int linesOfCode;
    private final int complexity;           // cyclomatic complexity of the file
    private final int todoCount;            // how many TODO/FIXME comments are left
    private final boolean hardcodedCredentials;
    private final List<String> riskyCalls;  // e.g. "String-concatenated SQL"

    public SourceCodeFile(String name, int linesOfCode, int complexity, int todoCount,
                          boolean hardcodedCredentials, List<String> riskyCalls) {
        this.name = name;
        this.linesOfCode = linesOfCode;
        this.complexity = complexity;
        this.todoCount = todoCount;
        this.hardcodedCredentials = hardcodedCredentials;
        this.riskyCalls = List.copyOf(riskyCalls);
    }

    public String getName() { return name; }
    public int getLinesOfCode() { return linesOfCode; }
    public int getComplexity() { return complexity; }
    public int getTodoCount() { return todoCount; }
    public boolean hasHardcodedCredentials() { return hardcodedCredentials; }
    public List<String> getRiskyCalls() { return riskyCalls; }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);   // 'this' is a SourceCodeFile here -> picks visit(SourceCodeFile)
    }
}
