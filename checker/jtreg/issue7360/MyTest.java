// Test for https://github.com/typetools/checker-framework/issues/7360 .
/*
 * @test
 * @summary Handle annotations for types loaded from bytecode.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.optional.OptionalChecker C.java W.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.optional.OptionalChecker MyTest.java
 */
class MyTest {
  public void from() {
    W.wrap(C.create());
  }
}
