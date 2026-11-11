package com.inspection;

import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;
import com.inspection.visitor.ProjectStatisticsVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectStatisticsVisitorTest {

    private static SourceCodeFile source(String name, int loc, int complexity) {
        return new SourceCodeFile(name, loc, complexity, 0, false, List.of());
    }

    @Test
    void calculatesAllNumbersForASmallProject() {
        Directory project = new Directory("proj",
                new Directory("src", source("A.java", 100, 4), source("B.java", 200, 8)),
                new Directory("tests", new TestFile("ATest.java", 50, 5, "A.java")),
                new DependencyFile("pom.xml", List.of(
                        new Dependency("a", "1"), new Dependency("b", "2"), new Dependency("c", "3"))));

        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        project.accept(stats);

        assertEquals(4, stats.getTotalFiles());          // 2 source + 1 test + 1 pom
        assertEquals(2, stats.getSourceFileCount());
        assertEquals(1, stats.getTestFileCount());
        assertEquals(350, stats.getTotalLinesOfCode());  // 300 source + 50 tests
        assertEquals(5, stats.getTestCount());
        assertEquals(3, stats.getDependencyCount());
        assertEquals(6.0, stats.getAverageComplexity(), 0.0001);
        assertEquals(0.5, stats.getTestSourceRatio(), 0.0001);
    }

    @Test
    void emptyDirectory_givesZerosAndNoNaN() {
        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        new Directory("empty").accept(stats);

        assertEquals(0, stats.getTotalFiles());
        assertEquals(0, stats.getTotalLinesOfCode());
        assertEquals(0.0, stats.getAverageComplexity());
        assertEquals(0.0, stats.getTestSourceRatio());
        assertFalse(Double.isNaN(stats.getAverageComplexity()));
    }

    @Test
    void onlyTestFiles_ratioIsZeroInsteadOfDividingByZero() {
        Directory project = new Directory("proj", new TestFile("ATest.java", 40, 3, "A.java"));

        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        project.accept(stats);

        assertEquals(1, stats.getTestFileCount());
        assertEquals(0.0, stats.getTestSourceRatio());
    }

    @Test
    void reportContainsTheCalculatedValues() {
        Directory project = new Directory("proj", source("A.java", 100, 4), source("B.java", 200, 8));

        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        project.accept(stats);
        String report = stats.getReport();

        assertTrue(report.contains("Average complexity: 6.00"));
        assertTrue(report.contains("Source files:     2"));
    }
}
