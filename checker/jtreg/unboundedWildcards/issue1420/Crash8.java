/*
 * @test
 * @summary Test for Issue 1420.
 * https://github.com/typetools/checker-framework/issues/1420
 *
 * @compile Crash8Lib.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker  Crash8.java
 */
abstract class Crash8<X extends Crash8Lib.Box<X>> {
  void test(Crash8Lib.Main main, boolean b, Class<X> cls) {
    Crash8Lib.MyIterable<X> x = main.foo(cls).get1().getIterable2();
    x = b ? x : main.foo(cls).get1().getIterable2();
  }
}
