package com.inspection.visitor;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.ProjectElement;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;

import java.util.Locale;

/**
 * Counts things in the project: files, lines, tests, dependencies,
 * average complexity and the test/source ratio.
 *
 * Create a new visitor for every run, the counters just keep adding up.
 */
public class ProjectStatisticsVisitor implements ProjectVisitor {

    private int directories;
    private int sourceFiles;
    private int testFiles;
    private int configFiles;
    private int dependencyFiles;

    private int sourceLines;
    private int testLines;
    private int testCount;
    private int dependencyCount;
    private int totalComplexity;

    @Override
    public void visit(SourceCodeFile file) {
        sourceFiles++;
        sourceLines += file.getLinesOfCode();
        totalComplexity += file.getComplexity();
    }

    @Override
    public void visit(TestFile file) {
        testFiles++;
        testLines += file.getLinesOfCode();
        testCount += file.getTestCount();
    }

    @Override
    public void visit(ConfigurationFile file) {
        configFiles++;
    }

    @Override
    public void visit(DependencyFile file) {
        dependencyFiles++;
        dependencyCount += file.getDependencies().size();
    }

    @Override
    public void visit(Directory directory) {
        directories++;
        for (ProjectElement child : directory.getChildren()) {
            child.accept(this);   // go deeper, works for nested directories too
        }
    }

    public int getDirectoryCount() { return directories; }
    public int getSourceFileCount() { return sourceFiles; }
    public int getTestFileCount() { return testFiles; }
    public int getConfigFileCount() { return configFiles; }
    public int getDependencyFileCount() { return dependencyFiles; }
    public int getSourceLines() { return sourceLines; }
    public int getTestLines() { return testLines; }
    public int getTestCount() { return testCount; }
    public int getDependencyCount() { return dependencyCount; }

    public int getTotalFiles() {
        return sourceFiles + testFiles + configFiles + dependencyFiles;
    }

    public int getTotalLinesOfCode() {
        return sourceLines + testLines;
    }

    /** Average over source files only. 0 if there are no source files (avoids dividing by zero). */
    public double getAverageComplexity() {
        if (sourceFiles == 0) {
            return 0.0;
        }
        return (double) totalComplexity / sourceFiles;
    }

    /** Test files per source file. 0 if there is no source code at all. */
    public double getTestSourceRatio() {
        if (sourceFiles == 0) {
            return 0.0;
        }
        return (double) testFiles / sourceFiles;
    }

    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== PROJECT STATISTICS ===\n");
        sb.append(String.format(Locale.US, "Directories:        %d%n", directories));
        sb.append(String.format(Locale.US, "Total files:        %d%n", getTotalFiles()));
        sb.append(String.format(Locale.US, "  Source files:     %d%n", sourceFiles));
        sb.append(String.format(Locale.US, "  Test files:       %d%n", testFiles));
        sb.append(String.format(Locale.US, "  Config files:     %d%n", configFiles));
        sb.append(String.format(Locale.US, "  Dependency files: %d%n", dependencyFiles));
        sb.append(String.format(Locale.US, "Lines of code:      %d (source %d, tests %d)%n",
                getTotalLinesOfCode(), sourceLines, testLines));
        sb.append(String.format(Locale.US, "Number of tests:    %d%n", testCount));
        sb.append(String.format(Locale.US, "Dependencies:       %d%n", dependencyCount));
        sb.append(String.format(Locale.US, "Average complexity: %.2f%n", getAverageComplexity()));
        sb.append(String.format(Locale.US, "Test/source ratio:  %.2f%n", getTestSourceRatio()));
        return sb.toString();
    }
}
