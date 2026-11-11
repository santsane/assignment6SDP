package com.inspection;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;
import com.inspection.visitor.ComplianceVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplianceVisitorTest {

    private static ComplianceVisitor check(Directory project) {
        ComplianceVisitor visitor = new ComplianceVisitor();
        project.accept(visitor);
        return visitor;
    }

    @Test
    void exposedCredentials_areFoundInSourceAndConfig() {
        Directory project = new Directory("p",
                new SourceCodeFile("A.java", 100, 5, 0, true, List.of()),
                new ConfigurationFile("app.properties", Map.of("db.password", "admin123")),
                new TestFile("ATest.java", 10, 1, "A.java"));

        ComplianceVisitor visitor = check(project);

        assertEquals(2, visitor.countViolations(ComplianceVisitor.NO_EXPOSED_CREDENTIALS));
        assertEquals(2, visitor.getViolations().size());   // nothing else is wrong
    }

    @Test
    void dependencyWithoutVersion_isAViolation() {
        DependencyFile pom = new DependencyFile("pom.xml", List.of(
                new Dependency("gson", null), new Dependency("jackson-databind", "2.15.2")));

        ComplianceVisitor visitor = check(new Directory("p", pom));

        assertEquals(1, visitor.countViolations(ComplianceVisitor.VERSION_REQUIRED));
        assertTrue(visitor.getViolations().get(0).message().contains("gson"));
    }

    @Test
    void prohibitedDependency_isAViolation() {
        DependencyFile pom = new DependencyFile("pom.xml", List.of(
                new Dependency("commons-httpclient", "3.1"), new Dependency("gson", "2.10.1")));

        ComplianceVisitor visitor = check(new Directory("p", pom));

        assertEquals(1, visitor.countViolations(ComplianceVisitor.NO_PROHIBITED_DEPENDENCIES));
    }

    @Test
    void sourceFileWithoutTest_isAViolation() {
        Directory project = new Directory("p",
                new SourceCodeFile("PaymentService.java", 100, 5, 0, false, List.of()),
                new SourceCodeFile("UserService.java", 100, 5, 0, false, List.of()),
                new TestFile("PaymentServiceTest.java", 50, 3, "PaymentService.java"));

        ComplianceVisitor visitor = check(project);

        assertEquals(1, visitor.countViolations(ComplianceVisitor.NEEDS_TESTS));
        assertEquals("UserService.java", visitor.getViolations().get(0).element());
    }

    @Test
    void testInAnotherDirectory_stillCountsAsCovered() {
        Directory project = new Directory("p",
                new Directory("src", new Directory("main",
                        new SourceCodeFile("A.java", 100, 5, 0, false, List.of()))),
                new Directory("tests", new Directory("unit",
                        new TestFile("ATest.java", 50, 3, "A.java"))));

        assertTrue(check(project).isCompliant());
    }

    @Test
    void cleanProject_isCompliant() {
        Directory project = new Directory("p",
                new SourceCodeFile("A.java", 100, 5, 0, false, List.of()),
                new TestFile("ATest.java", 50, 3, "A.java"),
                new ConfigurationFile("app.properties", Map.of("db.password", "${DB_PASSWORD}")),
                new DependencyFile("pom.xml", List.of(new Dependency("gson", "2.10.1"))));

        ComplianceVisitor visitor = check(project);

        assertTrue(visitor.isCompliant());
        assertTrue(visitor.getReport().contains("COMPLIANCE RESULT: COMPLIANT"));
    }

    @Test
    void askingForViolationsTwice_givesTheSameAnswer() {
        // untested-file violations are computed on demand, they must not pile up
        Directory project = new Directory("p",
                new SourceCodeFile("A.java", 100, 5, 0, false, List.of()));

        ComplianceVisitor visitor = check(project);

        assertEquals(1, visitor.getViolations().size());
        assertEquals(1, visitor.getViolations().size());
    }

    @Test
    void emptyDirectory_hasNothingToViolate() {
        assertTrue(check(new Directory("empty")).isCompliant());
    }
}
