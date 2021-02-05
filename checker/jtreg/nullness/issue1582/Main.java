/*
 * @test
 * @summary
 * Test case for Issue 1582
 * https://github.com/typetools/checker-framework/issues/1582
 *
 * @compile/fail/ref=Foo.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext foo/Foo.java
 * @compile -XDrawDiagnostics foo/Foo.java
 * @compile/fail/ref=JavaExpressionParseError.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext mainrepropkg/JavaExpressionParseError.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext mainrepropkg/JavaExpressionParseError.java -AsuppressWarnings=flowexpr.parse.error.postcondition
 *
 */

public class Main {}
