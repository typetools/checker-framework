/*
 * @test
 * @summary Test for Issue 1420.
 * https://github.com/typetools/checker-framework/issues/1420
 *
 * @compile B.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AprintErrorStack T.java
 */
class Test {
    {
        Three.c().g().f().build();
    }
}
