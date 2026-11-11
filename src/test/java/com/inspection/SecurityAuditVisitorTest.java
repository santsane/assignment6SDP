package com.inspection;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.SourceCodeFile;
import com.inspection.visitor.SecurityAuditVisitor;
import com.inspection.visitor.Severity;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAuditVisitorTest {

    private static SecurityAuditVisitor audit(Directory project) {
        SecurityAuditVisitor visitor = new SecurityAuditVisitor();
        project.accept(visitor);
        return visitor;
    }

    @Test
    void hardcodedCredentialsInSource_areCritical() {
        Directory project = new Directory("p",
                new SourceCodeFile("PaymentService.java", 100, 5, 0, true, List.of()));

        SecurityAuditVisitor visitor = audit(project);

        assertEquals(1, visitor.getCriticalCount());
        assertEquals(85, visitor.getScore());   // 100 - 15
    }

    @Test
    void exposedPasswordInConfig_isCriticalButEnvironmentPlaceholderIsFine() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("db.password", "admin123");           // bad
        settings.put("api.token", "${API_TOKEN}");         // fine, read from environment
        settings.put("server.port", "8080");               // not a secret at all

        SecurityAuditVisitor visitor = audit(new Directory("p",
                new ConfigurationFile("application.properties", settings)));

        assertEquals(1, visitor.getCriticalCount());
        assertTrue(visitor.getIssues().get(0).message().contains("db.password"));
    }

    @Test
    void insecureConfiguration_isFlaggedWithTheRightSeverity() {
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("ssl.enabled", "false");
        settings.put("debug", "true");
        settings.put("cors.allowed-origins", "*");

        SecurityAuditVisitor visitor = audit(new Directory("p",
                new ConfigurationFile("application.properties", settings)));

        assertEquals(1, visitor.countIssues(Severity.HIGH));
        assertEquals(2, visitor.countIssues(Severity.MEDIUM));
        assertEquals(0, visitor.getCriticalCount());
        assertEquals(88, visitor.getScore());   // 100 - 6 - 3 - 3
    }

    @Test
    void vulnerableDependencies_areFoundButSafeAndUnversionedOnesAreNot() {
        DependencyFile pom = new DependencyFile("pom.xml", List.of(
                new Dependency("log4j-core", "2.14.1"),         // known critical
                new Dependency("jackson-databind", "2.9.8"),    // known high
                new Dependency("log4j-core", "2.17.1"),         // patched version
                new Dependency("gson", null)));                 // no version, can't judge

        SecurityAuditVisitor visitor = audit(new Directory("p", pom));

        assertEquals(2, visitor.getIssues().size());
        assertEquals(1, visitor.getCriticalCount());
        assertEquals(1, visitor.countIssues(Severity.HIGH));
    }

    @Test
    void highRiskSourceFile_complexityBoundaryAndRiskyCalls() {
        SecurityAuditVisitor atLimit = audit(new Directory("p",
                new SourceCodeFile("Ok.java", 100, 15, 0, false, List.of())));
        SecurityAuditVisitor overLimit = audit(new Directory("p",
                new SourceCodeFile("Complex.java", 100, 16, 0, false, List.of())));
        SecurityAuditVisitor riskyCall = audit(new Directory("p",
                new SourceCodeFile("Sql.java", 100, 3, 0, false, List.of("String-concatenated SQL"))));

        assertEquals(0, atLimit.getIssues().size());    // 15 is still allowed
        assertEquals(1, overLimit.countIssues(Severity.HIGH));
        assertEquals(1, riskyCall.countIssues(Severity.HIGH));
    }

    @Test
    void cleanProject_getsFullScore() {
        Directory project = new Directory("p",
                new SourceCodeFile("A.java", 100, 5, 0, false, List.of()));

        SecurityAuditVisitor visitor = audit(project);

        assertEquals(100, visitor.getScore());
        assertEquals("LOW RISK", visitor.getRiskLevel());
        assertTrue(visitor.getReport().contains("No security issues found."));
    }

    @Test
    void scoreNeverGoesBelowZero() {
        Directory project = new Directory("p");
        for (int i = 0; i < 10; i++) {
            project.add(new SourceCodeFile("Bad" + i + ".java", 100, 5, 0, true, List.of()));
        }

        SecurityAuditVisitor visitor = audit(project);

        assertEquals(10, visitor.getCriticalCount());
        assertEquals(0, visitor.getScore());   // 150 points of penalty, but score stops at 0
        assertEquals("HIGH RISK", visitor.getRiskLevel());
    }

    @Test
    void issuesInNestedDirectories_areFound() {
        Directory project = new Directory("p",
                new Directory("a", new Directory("b",
                        new SourceCodeFile("Deep.java", 100, 5, 0, true, List.of()))));

        assertEquals(1, audit(project).getCriticalCount());
    }
}
