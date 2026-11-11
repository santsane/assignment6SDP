package com.inspection.element;

import com.inspection.visitor.ProjectVisitor;

/** A test file. testedFile is the name of the source file it covers. */
public class TestFile implements ProjectElement {

    private final String name;
    private final int linesOfCode;
    private final int testCount;       // number of @Test methods
    private final String testedFile;   // e.g. "PaymentService.java"

    public TestFile(String name, int linesOfCode, int testCount, String testedFile) {
        this.name = name;
        this.linesOfCode = linesOfCode;
        this.testCount = testCount;
        this.testedFile = testedFile;
    }

    public String getName() { return name; }
    public int getLinesOfCode() { return linesOfCode; }
    public int getTestCount() { return testCount; }
    public String getTestedFile() { return testedFile; }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
