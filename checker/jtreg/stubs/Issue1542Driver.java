/*
 * @test
 * @summary Test case for Issue 1542 https://github.com/typetools/checker-framework/issues/1542
 * @compile -XDrawDiagnostics issue1542/*.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.tainting.TaintingChecker -AprintErrorStack -Anomsgtext Issue1542Driver.java -Astubs=issue1542 -AstubWarnIfNotFound
 */

package issue1542;

public class Issue1542Driver {}
