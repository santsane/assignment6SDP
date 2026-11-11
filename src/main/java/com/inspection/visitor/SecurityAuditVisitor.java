package com.inspection.visitor;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.ProjectElement;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Looks for security problems: hardcoded credentials, vulnerable dependencies,
 * insecure configuration and high-risk source files.
 * Score starts at 100 and every issue takes points off (see Severity).
 */
public class SecurityAuditVisitor implements ProjectVisitor {

    // a source file with complexity above this is treated as high-risk
    static final int HIGH_RISK_COMPLEXITY = 15;

    // tiny "database" of known bad versions (name:version -> how bad)
    private static final Map<String, Severity> KNOWN_VULNERABLE = Map.of(
            "log4j-core:2.14.1", Severity.CRITICAL,        // Log4Shell
            "commons-text:1.9", Severity.HIGH,             // Text4Shell
            "jackson-databind:2.9.8", Severity.HIGH);

    private final List<SecurityIssue> issues = new ArrayList<>();

    @Override
    public void visit(SourceCodeFile file) {
        if (file.hasHardcodedCredentials()) {
            issues.add(new SecurityIssue(Severity.CRITICAL, file.getName(),
                    "hardcoded credentials found in source code"));
        }
        boolean tooComplex = file.getComplexity() > HIGH_RISK_COMPLEXITY;
        boolean usesRiskyCalls = !file.getRiskyCalls().isEmpty();
        if (tooComplex || usesRiskyCalls) {
            String why = "complexity " + file.getComplexity();
            if (usesRiskyCalls) {
                why += ", risky calls: " + String.join(", ", file.getRiskyCalls());
            }
            issues.add(new SecurityIssue(Severity.HIGH, file.getName(),
                    "high-risk source file (" + why + ")"));
        }
    }

    @Override
    public void visit(TestFile file) {
        // test files are not part of the security audit, nothing to check
    }

    @Override
    public void visit(ConfigurationFile file) {
        for (Map.Entry<String, String> setting : file.getSettings().entrySet()) {
            String key = setting.getKey();
            String value = setting.getValue();

            if (SecretRules.isExposedSecret(key, value)) {
                issues.add(new SecurityIssue(Severity.CRITICAL, file.getName(),
                        "hardcoded credential in setting '" + key + "'"));
            }
            if (key.equals("ssl.enabled") && "false".equalsIgnoreCase(value)) {
                issues.add(new SecurityIssue(Severity.HIGH, file.getName(),
                        "insecure configuration: SSL is disabled"));
            }
            if (key.equals("debug") && "true".equalsIgnoreCase(value)) {
                issues.add(new SecurityIssue(Severity.MEDIUM, file.getName(),
                        "insecure configuration: debug mode is on"));
            }
            if (key.equals("cors.allowed-origins") && "*".equals(value)) {
                issues.add(new SecurityIssue(Severity.MEDIUM, file.getName(),
                        "insecure configuration: CORS allows every origin"));
            }
        }
    }

    @Override
    public void visit(DependencyFile file) {
        for (Dependency dep : file.getDependencies()) {
            if (!dep.hasVersion()) {
                continue;   // no version -> can't tell, compliance visitor complains about that
            }
            Severity severity = KNOWN_VULNERABLE.get(dep.name() + ":" + dep.version());
            if (severity != null) {
                issues.add(new SecurityIssue(severity, file.getName(),
                        "vulnerable dependency " + dep.name() + ":" + dep.version()));
            }
        }
    }

    @Override
    public void visit(Directory directory) {
        for (ProjectElement child : directory.getChildren()) {
            child.accept(this);
        }
    }

    public List<SecurityIssue> getIssues() {
        return List.copyOf(issues);
    }

    public int countIssues(Severity severity) {
        return (int) issues.stream().filter(i -> i.severity() == severity).count();
    }

    public int getCriticalCount() {
        return countIssues(Severity.CRITICAL);
    }

    /** 100 = nothing found, never goes below 0. */
    public int getScore() {
        int penalty = issues.stream().mapToInt(i -> i.severity().getPenalty()).sum();
        return Math.max(0, 100 - penalty);
    }

    public String getRiskLevel() {
        int score = getScore();
        if (score >= 80) {
            return "LOW RISK";
        } else if (score >= 50) {
            return "MEDIUM RISK";
        }
        return "HIGH RISK";
    }

    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SECURITY AUDIT ===\n");
        if (issues.isEmpty()) {
            sb.append("No security issues found.\n");
        } else {
            // worst first (enum order is CRITICAL, HIGH, MEDIUM, LOW)
            issues.stream()
                    .sorted(Comparator.comparing(SecurityIssue::severity))
                    .forEach(issue -> sb.append(issue).append("\n"));
        }
        sb.append(String.format("Critical: %d | High: %d | Medium: %d | Low: %d%n",
                countIssues(Severity.CRITICAL), countIssues(Severity.HIGH),
                countIssues(Severity.MEDIUM), countIssues(Severity.LOW)));
        sb.append(String.format("Security score: %d/100 (%s)%n", getScore(), getRiskLevel()));
        return sb.toString();
    }
}
