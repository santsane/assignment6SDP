package com.inspection;

import com.inspection.element.ConfigurationFile;
import com.inspection.element.Dependency;
import com.inspection.element.DependencyFile;
import com.inspection.element.Directory;
import com.inspection.element.SourceCodeFile;
import com.inspection.element.TestFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the fake fintech project used for the demo (and in a couple of tests). */
public final class DemoProject {

    private DemoProject() {
    }

    /**
     * fintech-project/
     * ├── src/            PaymentService.java, UserService.java
     * ├── tests/          PaymentServiceTest.java
     * ├── config/         application.properties
     * └── pom.xml
     *
     * Full of problems on purpose so every visitor has something to report.
     */
    public static Directory fintechProject() {
        SourceCodeFile payment = new SourceCodeFile("PaymentService.java", 420, 18, 3,
                true, List.of());
        SourceCodeFile user = new SourceCodeFile("UserService.java", 210, 7, 1,
                false, List.of("String-concatenated SQL query"));
        Directory src = new Directory("src", payment, user);

        TestFile paymentTest = new TestFile("PaymentServiceTest.java", 180, 12, "PaymentService.java");
        Directory tests = new Directory("tests", paymentTest);

        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("server.port", "8080");
        settings.put("db.url", "jdbc:postgresql://localhost:5432/fintech");
        settings.put("db.password", "admin123");
        settings.put("ssl.enabled", "false");
        settings.put("debug", "true");
        Directory config = new Directory("config", new ConfigurationFile("application.properties", settings));

        DependencyFile pom = new DependencyFile("pom.xml", List.of(
                new Dependency("spring-core", "6.1.5"),
                new Dependency("jackson-databind", "2.15.2"),
                new Dependency("log4j-core", "2.14.1"),          // vulnerable
                new Dependency("commons-httpclient", "3.1"),     // prohibited
                new Dependency("gson", null),                    // no version
                new Dependency("junit-jupiter", "5.10.2")));

        return new Directory("fintech-project", src, tests, config, pom);
    }

    /** Same project after somebody fixed everything. Used to show a PASSED quality gate. */
    public static Directory fixedFintechProject() {
        SourceCodeFile payment = new SourceCodeFile("PaymentService.java", 280, 9, 0,
                false, List.of());
        SourceCodeFile user = new SourceCodeFile("UserService.java", 210, 7, 0,
                false, List.of());
        Directory src = new Directory("src", payment, user);

        Directory tests = new Directory("tests",
                new TestFile("PaymentServiceTest.java", 180, 12, "PaymentService.java"),
                new TestFile("UserServiceTest.java", 120, 8, "UserService.java"));

        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("server.port", "8080");
        settings.put("db.url", "jdbc:postgresql://localhost:5432/fintech");
        settings.put("db.password", "${DB_PASSWORD}");   // comes from the environment now
        settings.put("ssl.enabled", "true");
        settings.put("debug", "false");
        Directory config = new Directory("config", new ConfigurationFile("application.properties", settings));

        DependencyFile pom = new DependencyFile("pom.xml", List.of(
                new Dependency("spring-core", "6.1.5"),
                new Dependency("jackson-databind", "2.15.2"),
                new Dependency("log4j-core", "2.17.1"),
                new Dependency("gson", "2.10.1"),
                new Dependency("junit-jupiter", "5.10.2")));

        return new Directory("fintech-project", src, tests, config, pom);
    }
}
