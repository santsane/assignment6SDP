package com.inspection.visitor;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.ProjectElement;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks our (made-up) company rules:
 *  1. no exposed credentials
 *  2. dependency versions must be specified
 *  3. every production source file must have a test
 *  4. prohibited dependencies are not allowed
 *
 * Rule 3 can only be judged after the whole tree was visited (the test might be
 * in a different folder), so we collect names while visiting and compare in getViolations().
 */
public class ComplianceVisitor implements ProjectVisitor {

    public static final String NO_EXPOSED_CREDENTIALS = "NO_EXPOSED_CREDENTIALS";
    public static final String VERSION_REQUIRED = "DEPENDENCY_VERSION_REQUIRED";
    public static final String NEEDS_TESTS = "PRODUCTION_CODE_NEEDS_TESTS";
    public static final String NO_PROHIBITED_DEPENDENCIES = "NO_PROHIBITED_DEPENDENCIES";

    private static final Set<String> PROHIBITED = Set.of("log4j", "commons-httpclient");

    private final List<Violation> violations = new ArrayList<>();
    private final Set<String> sourceFileNames = new LinkedHashSet<>();
    private final Set<String> testedFileNames = new LinkedHashSet<>();

    @Override
    public void visit(SourceCodeFile file) {
        sourceFileNames.add(file.getName());
        if (file.hasHardcodedCredentials()) {
            violations.add(new Violation(NO_EXPOSED_CREDENTIALS, file.getName(),
                    "credentials are hardcoded in the source"));
        }
    }

    @Override
    public void visit(TestFile file) {
        testedFileNames.add(file.getTestedFile());
    }

    @Override
    public void visit(ConfigurationFile file) {
        for (Map.Entry<String, String> setting : file.getSettings().entrySet()) {
            if (SecretRules.isExposedSecret(setting.getKey(), setting.getValue())) {
                violations.add(new Violation(NO_EXPOSED_CREDENTIALS, file.getName(),
                        "setting '" + setting.getKey() + "' exposes a credential"));
            }
        }
    }

    @Override
    public void visit(DependencyFile file) {
        for (Dependency dep : file.getDependencies()) {
            if (!dep.hasVersion()) {
                violations.add(new Violation(VERSION_REQUIRED, file.getName(),
                        "dependency '" + dep.name() + "' has no version"));
            }
            if (PROHIBITED.contains(dep.name())) {
                violations.add(new Violation(NO_PROHIBITED_DEPENDENCIES, file.getName(),
                        "dependency '" + dep.name() + "' is prohibited"));
            }
        }
    }

    @Override
    public void visit(Directory directory) {
        for (ProjectElement child : directory.getChildren()) {
            child.accept(this);
        }
    }

    /** Builds the full list fresh each time, so it is safe to call more than once. */
    public List<Violation> getViolations() {
        List<Violation> all = new ArrayList<>(violations);
        for (String source : sourceFileNames) {
            if (!testedFileNames.contains(source)) {
                all.add(new Violation(NEEDS_TESTS, source, "no test file covers this source file"));
            }
        }
        return all;
    }

    public long countViolations(String rule) {
        return getViolations().stream().filter(v -> v.rule().equals(rule)).count();
    }

    public boolean isCompliant() {
        return getViolations().isEmpty();
    }

    public String getReport() {
        List<Violation> all = getViolations();
        StringBuilder sb = new StringBuilder();
        sb.append("=== COMPLIANCE CHECK ===\n");
        if (all.isEmpty()) {
            sb.append("All rules satisfied.\n");
        } else {
            for (Violation v : all) {
                sb.append(v).append("\n");
            }
        }
        sb.append("COMPLIANCE RESULT: ")
                .append(all.isEmpty() ? "COMPLIANT" : "NON-COMPLIANT (" + all.size() + " violations)")
                .append("\n");
        return sb.toString();
    }
}
