/*
 * @test
 * @summary Test for Issue 1427.
 * https://github.com/typetools/checker-framework/issues/1427
 *
 * @compile B.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker T.java
 */
class Test {
    {
        Three.c().g().f().build();
    }
}
