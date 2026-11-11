package com.inspection;

import com.inspection.element.Directory;
import com.inspection.visitor.ComplianceVisitor;
import com.inspection.visitor.ProjectStatisticsVisitor;
import com.inspection.visitor.QualityGateVisitor;
import com.inspection.visitor.SecurityAuditVisitor;

/** Runs every visitor on the same fintech project and prints the reports. */
public class Main {

    public static void main(String[] args) {
        Directory project = DemoProject.fintechProject();

        System.out.println("##########  INSPECTING: " + project.getName() + "  ##########\n");

        // same pattern every time: new visitor -> project.accept(visitor) -> read the result
        SecurityAuditVisitor security = new SecurityAuditVisitor();
        project.accept(security);
        System.out.println(security.getReport());

        ProjectStatisticsVisitor stats = new ProjectStatisticsVisitor();
        project.accept(stats);
        System.out.println(stats.getReport());

        QualityGateVisitor gate = new QualityGateVisitor();
        project.accept(gate);
        System.out.println(gate.getReport());

        ComplianceVisitor compliance = new ComplianceVisitor();
        project.accept(compliance);
        System.out.println(compliance.getReport());

        // and now the fixed version of the project, to show the gate can also PASS
        Directory fixed = DemoProject.fixedFintechProject();
        System.out.println("##########  AFTER FIXES  ##########\n");

        QualityGateVisitor fixedGate = new QualityGateVisitor();
        fixed.accept(fixedGate);
        System.out.println(fixedGate.getReport());

        ComplianceVisitor fixedCompliance = new ComplianceVisitor();
        fixed.accept(fixedCompliance);
        System.out.println(fixedCompliance.getReport());
    }
}
