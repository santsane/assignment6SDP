package com.inspection;

import com.inspection.element.Directory;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;
import com.inspection.visitor.QualityGateVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QualityGateVisitorTest {

    private static SourceCodeFile source(String name, int complexity, boolean credentials) {
        return new SourceCodeFile(name, 100, complexity, 0, credentials, List.of());
    }

    private static TestFile test(String name, String tested) {
        return new TestFile(name, 50, 5, tested);
    }

    private static QualityGateVisitor run(Directory project) {
        QualityGateVisitor gate = new QualityGateVisitor();
        project.accept(gate);
        return gate;
    }

    /** finds the report line that starts with the given text, e.g. "Tests:" */
    private static String line(String report, String start) {
        for (String l : report.split("\n")) {
            if (l.startsWith(start)) {
                return l;
            }
        }
        throw new AssertionError("no line starting with " + start);
    }

    @Test
    void goodProject_passesEverything() {
        Directory project = new Directory("p",
                source("A.java", 5, false), source("B.java", 7, false),
                test("ATest.java", "A.java"));

        QualityGateVisitor gate = run(project);

        assertTrue(gate.isSecurityPassed());
        assertTrue(gate.isComplexityPassed());
        assertTrue(gate.isTestsPassed());
        assertTrue(gate.isReleaseReady());
        assertTrue(gate.getReport().contains("FINAL RESULT: PASSED"));
    }

    @Test
    void notEnoughTests_failsOnlyTheTestsCriterion() {
        // 3 source files, 1 test file -> ratio 0.33 which is under 0.40
        Directory project = new Directory("p",
                source("A.java", 5, false), source("B.java", 5, false), source("C.java", 5, false),
                test("ATest.java", "A.java"));

        QualityGateVisitor gate = run(project);
        String report = gate.getReport();

        assertTrue(gate.isSecurityPassed());
        assertTrue(gate.isComplexityPassed());
        assertFalse(gate.isTestsPassed());
        assertTrue(line(report, "Tests:").contains("FAIL"));
        assertTrue(line(report, "Security:").contains("PASS"));
        assertTrue(report.contains("FINAL RESULT: FAILED"));
    }

    @Test
    void criticalSecurityIssue_failsTheGate() {
        Directory project = new Directory("p",
                source("A.java", 5, true),            // hardcoded credentials
                test("ATest.java", "A.java"));

        QualityGateVisitor gate = run(project);

        assertFalse(gate.isSecurityPassed());
        assertTrue(gate.isComplexityPassed());
        assertTrue(gate.isTestsPassed());
        assertFalse(gate.isReleaseReady());
    }

    @Test
    void tooComplex_failsTheGate() {
        Directory project = new Directory("p",
                source("A.java", 12, false), source("B.java", 14, false),   // average 13
                test("ATest.java", "A.java"));

        QualityGateVisitor gate = run(project);

        assertFalse(gate.isComplexityPassed());
        assertFalse(gate.isReleaseReady());
        assertTrue(line(gate.getReport(), "Complexity:").contains("FAIL"));
    }

    @Test
    void exactlyOnTheLimits_stillPasses() {
        // average complexity exactly 10 and ratio exactly 2/5 = 0.40 -> both are allowed (<= and >=)
        Directory project = new Directory("p",
                source("A.java", 10, false), source("B.java", 10, false), source("C.java", 10, false),
                source("D.java", 10, false), source("E.java", 10, false),
                test("ATest.java", "A.java"), test("BTest.java", "B.java"));

        assertTrue(run(project).isReleaseReady());
    }

    @Test
    void customThresholds_areRespected() {
        Directory project = new Directory("p",
                source("A.java", 6, false), test("ATest.java", "A.java"));

        QualityGateVisitor strict = new QualityGateVisitor(5.0, 0.9, 0);
        project.accept(strict);

        assertFalse(strict.isComplexityPassed());   // 6 > 5
        assertTrue(strict.isTestsPassed());         // 1.0 >= 0.9
    }

    @Test
    void emptyProject_isNotReadyForRelease() {
        QualityGateVisitor gate = run(new Directory("empty"));

        assertTrue(gate.isSecurityPassed());
        assertTrue(gate.isComplexityPassed());
        assertFalse(gate.isTestsPassed());          // ratio 0 is below 0.40
        assertFalse(gate.isReleaseReady());
    }
}
