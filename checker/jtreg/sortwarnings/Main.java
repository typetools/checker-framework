/*
 * @test
 * @summary Test that errors from compound checkers are sorted correctly.
 *
 * @compile/fail/ref=ErrorOrders.out -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker -AprintErrorStack OrderOfCheckers.java ErrorOrders.java -Anomsgtext -AshowSuppressWarningKeys
 * @compile/fail/ref=OrderOfCheckers.out -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker -AprintErrorStack OrderOfCheckers.java -AshowSuppressWarningKeys
 */

class Main {}
