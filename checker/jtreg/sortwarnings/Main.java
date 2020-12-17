/*
 * @test
 * @summary Test that errors from compound checkers are sorted correctly.
 * The first compilation below tests that the errors are ordered by line/column
 * number.  The second line tests that the errors at the same line/column are
 * sorted in order of checker; hence, -Anomsgtext is not passed so that
 * -AshowSuppressWarningsStrings has an effect.
 *
 * @compile/fail/ref=ErrorOrders.out -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker OrderOfCheckers.java ErrorOrders.java
 * @compile/fail/ref=OrderOfCheckers.out -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker OrderOfCheckers.java -AshowSuppressWarningsStrings
 */

public class Main {}
