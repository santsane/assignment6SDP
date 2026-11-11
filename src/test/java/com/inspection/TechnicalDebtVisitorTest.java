package com.inspection;

import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;
import com.inspection.visitor.TechnicalDebtVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for my extra visitor. */
class TechnicalDebtVisitorTest {

    private static TechnicalDebtVisitor run(Directory project) {
        TechnicalDebtVisitor visitor = new TechnicalDebtVisitor();
        project.accept(visitor);
        return visitor;
    }

    private static SourceCodeFile source(String name, int loc, int complexity, int todos) {
        return new SourceCodeFile(name, loc, complexity, todos, false, List.of());
    }

    @Test
    void complexityAboveTen_costsHalfAnHourPerExtraPoint() {
        Directory project = new Directory("p",
                source("A.java", 100, 14, 0),
                new TestFile("ATest.java", 10, 1, "A.java"));

        assertEquals(2.0, run(project).getTotalDebtHours(), 0.0001);   // (14 - 10) * 0.5
    }

    @Test
    void bigFilesAndTodos_addUp() {
        Directory project = new Directory("p",
                source("A.java", 301, 5, 2),
                new TestFile("ATest.java", 10, 1, "A.java"));

        assertEquals(3.0, run(project).getTotalDebtHours(), 0.0001);   // 2h big file + 2 * 0.5h todos
    }

    @Test
    void fileExactlyOnTheLimits_hasNoDebt() {
        Directory project = new Directory("p",
                source("A.java", 300, 10, 0),
                new TestFile("ATest.java", 10, 1, "A.java"));

        assertEquals(0.0, run(project).getTotalDebtHours(), 0.0001);
    }

    @Test
    void sourceFileWithoutTest_costsFourHours() {
        Directory project = new Directory("p", source("A.java", 100, 5, 0));

        TechnicalDebtVisitor visitor = run(project);

        assertEquals(4.0, visitor.getTotalDebtHours(), 0.0001);
        assertEquals(4.0, visitor.getDebtByElement().get("A.java"), 0.0001);
    }

    @Test
    void dependencyWithoutVersion_costsHalfAnHour() {
        DependencyFile pom = new DependencyFile("pom.xml", List.of(
                new Dependency("gson", null), new Dependency("guava", ""), new Dependency("junit", "5")));

        assertEquals(1.0, run(new Directory("p", pom)).getTotalDebtHours(), 0.0001);
    }

    @Test
    void ratingGetsWorseWithMoreDebt() {
        TechnicalDebtVisitor clean = run(new Directory("empty"));

        Directory messy = new Directory("messy");
        for (int i = 0; i < 11; i++) {
            messy.add(source("Untested" + i + ".java", 100, 5, 0));   // 11 * 4h = 44h
        }
        TechnicalDebtVisitor bad = run(messy);

        assertEquals("A", clean.getRating());
        assertEquals("D", bad.getRating());
    }

    @Test
    void emptyDirectory_hasNoDebt() {
        TechnicalDebtVisitor visitor = run(new Directory("empty"));

        assertEquals(0.0, visitor.getTotalDebtHours(), 0.0001);
        assertTrue(visitor.getReport().contains("No technical debt found."));
    }

    @Test
    void nestedDirectories_areIncluded() {
        Directory project = new Directory("p",
                new Directory("a", new Directory("b", new Directory("c",
                        source("Deep.java", 100, 12, 0)))),
                new Directory("tests", new TestFile("DeepTest.java", 10, 1, "Deep.java")));

        assertEquals(1.0, run(project).getTotalDebtHours(), 0.0001);   // (12 - 10) * 0.5
    }
}
