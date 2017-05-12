/*
 * @test
 * @summary Test case for Issue 1275.
 * https://github.com/typetools/checker-framework/issues/1275
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker#NullnessChecker -AprintErrorStack -Anomsgtext Lib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker#NullnessChecker -AprintErrorStack -Anomsgtext Crash.java
 */

class Main {}
