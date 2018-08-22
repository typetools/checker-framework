/*
 * @test
 * @summary Test case for Issue 1542 https://github.com/typetools/checker-framework/issues/1542
 *
 * @compile -XDrawDiagnostics issue1542/NeedsIntRange.java issue1542/Stub.java issue1542/ExampleAnno.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.common.value.ValueChecker issue1542/UsesIntRange.java -Astubs=issue1542/ -AstubWarnIfNotFound -Werror
 */

public class Issue1542Driver {}
