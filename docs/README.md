# Smart Code Inspection & Compliance Engine

**Assignment 6 - Visitor Pattern** (Java 17+, Maven, JUnit 5)
**Author:** YOUR NAME HERE

A small engine that inspects a software project (source files, tests, config files, dependency files and
directories) and runs different operations on it: security audit, statistics, quality gate, compliance
check and technical debt. The project elements and the operations are kept apart using the **Visitor Pattern**.

## How to run

Needs Java 17+ and Maven.

```
mvn test                  # runs all 45 JUnit 5 tests
mvn compile exec:java     # runs the demo (all visitors on fintech-project)
```

## Project structure

```
src/main/java/com/inspection/
├── element/                      <- the things being inspected (just data + accept())
│   ├── ProjectElement.java       (interface)
│   ├── SourceCodeFile.java
│   ├── TestFile.java
│   ├── ConfigurationFile.java
│   ├── DependencyFile.java
│   ├── Directory.java            (holds other ProjectElements, can be nested)
│   └── Dependency.java          (small record used by DependencyFile)
├── visitor/                      <- the operations
│   ├── ProjectVisitor.java       (interface)
│   ├── SecurityAuditVisitor.java
│   ├── ProjectStatisticsVisitor.java
│   ├── QualityGateVisitor.java
│   ├── ComplianceVisitor.java
│   ├── TechnicalDebtVisitor.java (my extra visitor)
│   └── Severity, SecurityIssue, Violation, SecretRules (small helpers)
├── DemoProject.java              builds the fake fintech-project
└── Main.java                     runs all visitors on it
src/test/java/com/inspection/     45 tests
docs/                             PlantUML sources + rendered diagrams
```

## The visitors

| Visitor | What it does |
|---|---|
| `SecurityAuditVisitor` | Finds hardcoded credentials (source + config), vulnerable dependencies, insecure config (SSL off, debug on, CORS `*`) and high-risk source files (complexity > 15 or risky calls). Gives a score out of 100 (CRITICAL -15, HIGH -6, MEDIUM -3, LOW -1). |
| `ProjectStatisticsVisitor` | Total files, lines of code, number of tests, dependencies, average complexity, test/source ratio (test files per source file). |
| `QualityGateVisitor` | Release decision: average complexity <= 10, test/source ratio >= 0.40, critical security issues = 0. Prints PASS/FAIL for each and a FINAL RESULT. Reuses the statistics and security visitors inside. |
| `ComplianceVisitor` | 4 rules: no exposed credentials, dependency versions must be specified, every source file needs a test, no prohibited dependencies (`log4j`, `commons-httpclient`). |
| `TechnicalDebtVisitor` | **My extra visitor.** Estimates cleanup hours: complexity above 10, files over 300 lines, TODOs, source files without tests, dependencies without versions. Gives a rating A-D. Added in its own commit without changing any element class. |

## Demo output

This is the real output of the demo (`Main`). The demo project is:

```
fintech-project/
├── src/      PaymentService.java, UserService.java
├── tests/    PaymentServiceTest.java
├── config/   application.properties
└── pom.xml
```

It has problems on purpose so every visitor has something to report. At the end the same checks run
on a fixed copy of the project to show that the quality gate can also pass.

```
##########  INSPECTING: fintech-project  ##########

=== SECURITY AUDIT ===
[CRITICAL] PaymentService.java - hardcoded credentials found in source code
[CRITICAL] application.properties - hardcoded credential in setting 'db.password'
[CRITICAL] pom.xml - vulnerable dependency log4j-core:2.14.1
[HIGH] PaymentService.java - high-risk source file (complexity 18)
[HIGH] UserService.java - high-risk source file (complexity 7, risky calls: String-concatenated SQL query)
[HIGH] application.properties - insecure configuration: SSL is disabled
[MEDIUM] application.properties - insecure configuration: debug mode is on
Critical: 3 | High: 3 | Medium: 1 | Low: 0
Security score: 34/100 (HIGH RISK)

=== PROJECT STATISTICS ===
Directories:        4
Total files:        5
  Source files:     2
  Test files:       1
  Config files:     1
  Dependency files: 1
Lines of code:      810 (source 630, tests 180)
Number of tests:    12
Dependencies:       6
Average complexity: 12.50
Test/source ratio:  0.50

=== QUALITY GATE ===
Security:   FAIL (critical issues: 3, allowed: 0)
Complexity: FAIL (average 12.50, max allowed 10.00)
Tests:      PASS (test/source ratio 0.50, min required 0.40)
FINAL RESULT: FAILED

=== COMPLIANCE CHECK ===
[NO_EXPOSED_CREDENTIALS] PaymentService.java - credentials are hardcoded in the source
[NO_EXPOSED_CREDENTIALS] application.properties - setting 'db.password' exposes a credential
[NO_PROHIBITED_DEPENDENCIES] pom.xml - dependency 'commons-httpclient' is prohibited
[DEPENDENCY_VERSION_REQUIRED] pom.xml - dependency 'gson' has no version
[PRODUCTION_CODE_NEEDS_TESTS] UserService.java - no test file covers this source file
COMPLIANCE RESULT: NON-COMPLIANT (5 violations)

=== TECHNICAL DEBT ===
PaymentService.java      7.5 h
UserService.java         4.5 h
pom.xml                  0.5 h
Total debt: 12.5 hours (rating B)

##########  AFTER FIXES  ##########

=== QUALITY GATE ===
Security:   PASS (critical issues: 0, allowed: 0)
Complexity: PASS (average 8.00, max allowed 10.00)
Tests:      PASS (test/source ratio 1.00, min required 0.40)
FINAL RESULT: PASSED

=== COMPLIANCE CHECK ===
All rules satisfied.
COMPLIANCE RESULT: COMPLIANT
```

## UML

**Class diagram** (source: `docs/class-diagram.puml`)

![Class diagram](docs/class-diagram.png)

**Sequence diagram** (source: `docs/sequence-diagram.puml`)

![Sequence diagram](docs/sequence-diagram.png)

## Questions

### What problem does Visitor solve in my project?
The same project elements need many different operations (security, statistics, quality gate, compliance,
technical debt). Without Visitor I would have to put `checkSecurity()`, `calculateStatistics()`,
`checkCompliance()` and so on inside every element class. With Visitor the elements only hold data and
have one method, `accept()`, and every operation is its own class.

### Where are Element and Visitor?
- Element: `com.inspection.element.ProjectElement` with the 5 concrete classes `SourceCodeFile`, `TestFile`,
  `ConfigurationFile`, `DependencyFile` and `Directory`.
- Visitor: `com.inspection.visitor.ProjectVisitor` with the 5 concrete visitors listed above.

### What is double dispatch?
The code that runs depends on the types of **two** objects: the element and the visitor. Java only picks
a method by the runtime type of one object (the one before the dot), so we do it in two steps:

1. `element.accept(visitor)` - Java picks the right `accept()` using the real type of the element.
2. Inside that `accept()`, `visitor.visit(this)` is called. There `this` has a concrete type
   (for example `SourceCodeFile`), so the compiler picks the right `visit(...)` overload, and the visitor's real
   class decides which implementation runs.

So the final method depends on both the element type and the visitor type, with no `instanceof` and
no big `switch`. The sequence diagram shows this.

### Why not put all operations directly inside the Element classes?
- Every new operation would mean editing all 5 element classes (breaks Open/Closed).
- The elements would get messy: a source file would know about security, statistics, compliance and debt (breaks Single Responsibility).
- Some operations need information from the whole tree. For example, "does this source file have a test?" depends on test files in other folders. That fits a visitor that walks the whole tree, not one element.

### Is it easier to add a new Visitor or a new Element? Why?
A new **Visitor** is easier. I did it with `TechnicalDebtVisitor`: one new class (plus its test and a
few lines in `Main`) and zero changes to the element classes. A new **Element** is harder because
`ProjectVisitor` gets a new method and every existing visitor has to be changed (see the next section).

### When would Visitor be a bad choice?
- When the element types change often (every new element touches every visitor).
- When there are only one or two operations, because the pattern adds a lot of extra classes for little gain.
- When visitors need private data of the elements. Elements have to expose getters, which weakens encapsulation.

### How is Visitor different from Strategy?
- **Strategy** swaps one algorithm inside one object. The algorithms are interchangeable and all work on the same kind of input (like different sorting or discount strategies). The client picks one.
- **Visitor** adds new operations to a whole structure of **different** element types, with different behavior per type (one `visit()` overload per type), and it needs double dispatch for that.

In short: Strategy = "which way to do this one thing", Visitor = "do many different things to many different kinds of objects".

### Which SOLID principles can I identify?
- **S - Single Responsibility:** each visitor has one job; elements only hold data.
- **O - Open/Closed:** I can add new operations (like `TechnicalDebtVisitor`) without touching any element. This only works in one direction: adding new *elements* does force changes in the visitors.
- **L - Liskov Substitution:** any `ProjectElement` can go inside a `Directory` and any `ProjectVisitor` can visit it. For example `QualityGateVisitor` works anywhere a `ProjectVisitor` is expected.
- **I - Interface Segregation:** `ProjectElement` is tiny (one method). `ProjectVisitor` is a fat interface (5 methods), so this one is only partly followed. Some visitors have empty `visit()` methods for elements they don't care about.
- **D - Dependency Inversion:** `Directory` depends on the `ProjectElement` interface and elements depend on the `ProjectVisitor` interface, not on concrete visitors.

## Visitor Limitation Challenge: what if we add `DatabaseMigrationFile`?

Things that **must change**:

1. **New class** `DatabaseMigrationFile implements ProjectElement` with `accept()` that calls `visitor.visit(this)`.
2. **`ProjectVisitor`** gets a new method `void visit(DatabaseMigrationFile file);`.
3. **All 5 existing visitors** must implement the new method or they won't compile: `SecurityAuditVisitor`, `ProjectStatisticsVisitor`, `QualityGateVisitor` (it forwards to the stats and security visitors), `ComplianceVisitor` and `TechnicalDebtVisitor`.
4. The `RecordingVisitor` inside `ElementTest` also implements `ProjectVisitor`, so it needs the new method too.

Things that do **not** change: the 5 existing element classes (including `Directory`, which only stores `ProjectElement`s) and `Main`'s overall flow.

This is the main weakness of Visitor: the element structure is hard to change. It is a good fit here because new *operations* are expected often (new rules, new audits) while the set of file types changes rarely. A possible workaround is giving `ProjectVisitor` default empty methods or an abstract base visitor, but then the compiler no longer forces visitors to handle new element types, which can hide bugs.

## Notes / design decisions
- `accept()` only calls `visitor.visit(this)`. Walking into a `Directory`'s children is done by each visitor.
- No `instanceof` and no `switch` on element types anywhere in the main code.
- The test/source ratio counts test files per source file. An empty project has ratio 0, so it fails the quality gate on purpose.
- Visitors keep state while visiting, so create a fresh visitor for every run.
