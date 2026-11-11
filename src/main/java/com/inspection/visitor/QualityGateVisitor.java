package com.inspection.visitor;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.ProjectElement;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;

import java.util.Locale;

/**
 * Decides if the project is ready for release.
 *
 * Instead of copy-pasting counting logic, it reuses the statistics and security
 * visitors: every visit() just forwards the element to them, and the gate only
 * looks at their results at the end.
 *
 * Criteria (defaults from the assignment):
 *   average complexity <= 10, test/source ratio >= 0.40, critical security issues = 0
 */
public class QualityGateVisitor implements ProjectVisitor {

    private final double maxAverageComplexity;
    private final double minTestRatio;
    private final int maxCriticalIssues;

    private final ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
    private final SecurityAuditVisitor security = new SecurityAuditVisitor();

    public QualityGateVisitor() {
        this(10.0, 0.40, 0);
    }

    public QualityGateVisitor(double maxAverageComplexity, double minTestRatio, int maxCriticalIssues) {
        this.maxAverageComplexity = maxAverageComplexity;
        this.minTestRatio = minTestRatio;
        this.maxCriticalIssues = maxCriticalIssues;
    }

    @Override
    public void visit(SourceCodeFile file) {
        stats.visit(file);
        security.visit(file);
    }

    @Override
    public void visit(TestFile file) {
        stats.visit(file);
        security.visit(file);
    }

    @Override
    public void visit(ConfigurationFile file) {
        stats.visit(file);
        security.visit(file);
    }

    @Override
    public void visit(DependencyFile file) {
        stats.visit(file);
        security.visit(file);
    }

    @Override
    public void visit(Directory directory) {
        // we walk the children ourselves with "this", so they all end up in visit() above
        for (ProjectElement child : directory.getChildren()) {
            child.accept(this);
        }
    }

    public boolean isSecurityPassed() {
        return security.getCriticalCount() <= maxCriticalIssues;
    }

    public boolean isComplexityPassed() {
        return stats.getAverageComplexity() <= maxAverageComplexity;
    }

    /** An empty project has ratio 0, so it fails here on purpose (nothing is tested). */
    public boolean isTestsPassed() {
        return stats.getTestSourceRatio() >= minTestRatio;
    }

    public boolean isReleaseReady() {
        return isSecurityPassed() && isComplexityPassed() && isTestsPassed();
    }

    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== QUALITY GATE ===\n");
        sb.append(String.format(Locale.US, "Security:   %s (critical issues: %d, allowed: %d)%n",
                label(isSecurityPassed()), security.getCriticalCount(), maxCriticalIssues));
        sb.append(String.format(Locale.US, "Complexity: %s (average %.2f, max allowed %.2f)%n",
                label(isComplexityPassed()), stats.getAverageComplexity(), maxAverageComplexity));
        sb.append(String.format(Locale.US, "Tests:      %s (test/source ratio %.2f, min required %.2f)%n",
                label(isTestsPassed()), stats.getTestSourceRatio(), minTestRatio));
        sb.append("FINAL RESULT: ").append(isReleaseReady() ? "PASSED" : "FAILED").append("\n");
        return sb.toString();
    }

    private String label(boolean passed) {
        return passed ? "PASS" : "FAIL";
    }
}
