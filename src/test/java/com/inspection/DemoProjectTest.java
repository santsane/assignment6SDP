package com.inspection;

import com.inspection.element.Directory;
import com.inspection.visitor.ComplianceVisitor;
import com.inspection.visitor.ProjectStatisticsVisitor;
import com.inspection.visitor.QualityGateVisitor;
import com.inspection.visitor.SecurityAuditVisitor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs every visitor on the same fintech project, like the demo does. */
class DemoProjectTest {

    @Test
    void allVisitorsOnTheFintechProject_giveTheExpectedResults() {
        Directory project = DemoProject.fintechProject();

        SecurityAuditVisitor security = new SecurityAuditVisitor();
        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        QualityGateVisitor gate = new QualityGateVisitor();
        ComplianceVisitor compliance = new ComplianceVisitor();

        project.accept(security);
        project.accept(stats);
        project.accept(gate);
        project.accept(compliance);

        assertEquals(3, security.getCriticalCount());
        assertEquals(34, security.getScore());

        assertEquals(5, stats.getTotalFiles());
        assertEquals(810, stats.getTotalLinesOfCode());
        assertEquals(12, stats.getTestCount());
        assertEquals(6, stats.getDependencyCount());
        assertEquals(12.5, stats.getAverageComplexity(), 0.0001);
        assertEquals(0.5, stats.getTestSourceRatio(), 0.0001);

        assertFalse(gate.isSecurityPassed());
        assertFalse(gate.isComplexityPassed());
        assertTrue(gate.isTestsPassed());
        assertFalse(gate.isReleaseReady());

        assertEquals(5, compliance.getViolations().size());
    }

    @Test
    void fixedProject_passesTheGateAndIsCompliant() {
        Directory fixed = DemoProject.fixedFintechProject();

        QualityGateVisitor gate = new QualityGateVisitor();
        ComplianceVisitor compliance = new ComplianceVisitor();
        fixed.accept(gate);
        fixed.accept(compliance);

        assertTrue(gate.isReleaseReady());
        assertTrue(compliance.isCompliant());
    }

    @Test
    void oneVisitorObjectCanBeUsedOnTwoProjects_countersJustKeepAdding() {
        // not a feature, just documenting why Main creates a fresh visitor for every run
        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        DemoProject.fintechProject().accept(stats);
        DemoProject.fintechProject().accept(stats);

        assertEquals(10, stats.getTotalFiles());
    }
}
