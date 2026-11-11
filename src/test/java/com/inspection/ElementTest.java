package com.inspection;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;
import com.inspection.visitor.ProjectStatisticsVisitor;
import com.inspection.visitor.ProjectVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the element side: double dispatch, directories, nesting, empty directory. */
class ElementTest {

    /** Fake visitor that only writes down which visit() overload was called. */
    static class RecordingVisitor implements ProjectVisitor {
        final List<String> calls = new ArrayList<>();

        public void visit(SourceCodeFile file) { calls.add("SourceCodeFile"); }
        public void visit(TestFile file) { calls.add("TestFile"); }
        public void visit(ConfigurationFile file) { calls.add("ConfigurationFile"); }
        public void visit(DependencyFile file) { calls.add("DependencyFile"); }
        public void visit(Directory directory) { calls.add("Directory"); }
    }

    @Test
    void accept_callsTheMatchingVisitOverloadForEachElementType() {
        // this is the double dispatch test: same call (accept), different visit() every time
        RecordingVisitor visitor = new RecordingVisitor();

        new SourceCodeFile("A.java", 10, 1, 0, false, List.of()).accept(visitor);
        new TestFile("ATest.java", 10, 1, "A.java").accept(visitor);
        new ConfigurationFile("app.properties", Map.of()).accept(visitor);
        new DependencyFile("pom.xml", List.of()).accept(visitor);
        new Directory("src").accept(visitor);

        assertEquals(List.of("SourceCodeFile", "TestFile", "ConfigurationFile",
                "DependencyFile", "Directory"), visitor.calls);
    }

    @Test
    void accept_onDirectoryOnlyVisitsTheDirectoryItself() {
        // the traversal is the visitor's job, so the recording visitor sees just one call
        Directory dir = new Directory("src", new SourceCodeFile("A.java", 10, 1, 0, false, List.of()));
        RecordingVisitor visitor = new RecordingVisitor();

        dir.accept(visitor);

        assertEquals(List.of("Directory"), visitor.calls);
    }

    @Test
    void elementsKeepTheDataTheyWereCreatedWith() {
        SourceCodeFile file = new SourceCodeFile("PaymentService.java", 420, 18, 3,
                true, List.of("eval"));

        assertEquals("PaymentService.java", file.getName());
        assertEquals(420, file.getLinesOfCode());
        assertEquals(18, file.getComplexity());
        assertEquals(3, file.getTodoCount());
        assertTrue(file.hasHardcodedCredentials());
        assertEquals(List.of("eval"), file.getRiskyCalls());
    }

    @Test
    void dependencyWithoutVersionIsDetected() {
        assertTrue(new Dependency("gson", "2.10").hasVersion());
        assertFalse(new Dependency("gson", null).hasVersion());
        assertFalse(new Dependency("gson", "  ").hasVersion());
    }

    @Test
    void nestedDirectories_areAllReachedByAVisitor() {
        Directory deepest = new Directory("util",
                new SourceCodeFile("Helper.java", 50, 2, 0, false, List.of()));
        Directory middle = new Directory("service", deepest);
        Directory root = new Directory("src", middle,
                new SourceCodeFile("Main.java", 30, 1, 0, false, List.of()));

        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        root.accept(stats);

        assertEquals(3, stats.getDirectoryCount());   // src, service, util
        assertEquals(2, stats.getSourceFileCount());
        assertEquals(80, stats.getSourceLines());
    }

    @Test
    void veryDeepNesting_stillWorks() {
        Directory current = new Directory("level-0",
                new SourceCodeFile("Bottom.java", 7, 1, 0, false, List.of()));
        for (int i = 1; i <= 50; i++) {
            current = new Directory("level-" + i, current);
        }

        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        current.accept(stats);

        assertEquals(51, stats.getDirectoryCount());
        assertEquals(1, stats.getSourceFileCount());
    }

    @Test
    void emptyDirectory_isVisitedWithoutProblems() {
        Directory empty = new Directory("empty");
        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();

        empty.accept(stats);

        assertEquals(1, stats.getDirectoryCount());
        assertEquals(0, stats.getTotalFiles());
    }
}
