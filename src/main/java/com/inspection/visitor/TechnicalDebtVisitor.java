package com.inspection.visitor;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.ProjectElement;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * My own extra visitor: estimates technical debt in hours of cleanup work.
 *
 * It was added AFTER everything else and no element class was touched,
 * it only uses data the elements already had (complexity, lines, TODOs, versions, tests).
 *
 * Debt rules (made up but easy to explain):
 *   complexity above 10      -> 0.5h for every extra point
 *   more than 300 lines      -> 2h (file should be split)
 *   each TODO/FIXME          -> 0.5h
 *   source file with no test -> 4h
 *   dependency with no version -> 0.5h
 */
public class TechnicalDebtVisitor implements ProjectVisitor {

    static final int COMPLEXITY_LIMIT = 10;
    static final int BIG_FILE_LINES = 300;
    static final double HOURS_PER_COMPLEXITY_POINT = 0.5;
    static final double HOURS_BIG_FILE = 2.0;
    static final double HOURS_PER_TODO = 0.5;
    static final double HOURS_NO_TEST = 4.0;
    static final double HOURS_NO_VERSION = 0.5;

    // debt found while visiting, per element name
    private final Map<String, Double> debtByElement = new LinkedHashMap<>();
    private final Set<String> sourceFileNames = new LinkedHashSet<>();
    private final Set<String> testedFileNames = new LinkedHashSet<>();

    @Override
    public void visit(SourceCodeFile file) {
        sourceFileNames.add(file.getName());

        double hours = 0;
        if (file.getComplexity() > COMPLEXITY_LIMIT) {
            hours += (file.getComplexity() - COMPLEXITY_LIMIT) * HOURS_PER_COMPLEXITY_POINT;
        }
        if (file.getLinesOfCode() > BIG_FILE_LINES) {
            hours += HOURS_BIG_FILE;
        }
        hours += file.getTodoCount() * HOURS_PER_TODO;

        debtByElement.merge(file.getName(), hours, Double::sum);
    }

    @Override
    public void visit(TestFile file) {
        testedFileNames.add(file.getTestedFile());
    }

    @Override
    public void visit(ConfigurationFile file) {
        // config files don't add debt in my rules
    }

    @Override
    public void visit(DependencyFile file) {
        double hours = 0;
        for (Dependency dep : file.getDependencies()) {
            if (!dep.hasVersion()) {
                hours += HOURS_NO_VERSION;
            }
        }
        if (hours > 0) {
            debtByElement.merge(file.getName(), hours, Double::sum);
        }
    }

    @Override
    public void visit(Directory directory) {
        for (ProjectElement child : directory.getChildren()) {
            child.accept(this);
        }
    }

    /** Debt per element including the "no test" penalty (worked out fresh each call). */
    public Map<String, Double> getDebtByElement() {
        Map<String, Double> result = new LinkedHashMap<>(debtByElement);
        for (String source : sourceFileNames) {
            if (!testedFileNames.contains(source)) {
                result.merge(source, HOURS_NO_TEST, Double::sum);
            }
        }
        return result;
    }

    public double getTotalDebtHours() {
        return getDebtByElement().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /** A is best, D is worst. */
    public String getRating() {
        double total = getTotalDebtHours();
        if (total <= 8) {
            return "A";
        } else if (total <= 20) {
            return "B";
        } else if (total <= 40) {
            return "C";
        }
        return "D";
    }

    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== TECHNICAL DEBT ===\n");
        Map<String, Double> debt = getDebtByElement();
        if (debt.isEmpty()) {
            sb.append("No technical debt found.\n");
        } else {
            debt.forEach((name, hours) ->
                    sb.append(String.format(Locale.US, "%-24s %.1f h%n", name, hours)));
        }
        sb.append(String.format(Locale.US, "Total debt: %.1f hours (rating %s)%n",
                getTotalDebtHours(), getRating()));
        return sb.toString();
    }
}
