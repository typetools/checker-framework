/*
 * @test
 * @summary Test for bug where arguments to constructors were visited twice.
 *
 * @compile/fail/ref=NewClass.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -Alint NewClass.java
 */

import org.checkerframework.checker.tainting.qual.Untainted;

public class NewClass {
  public NewClass(Object param) {}

  Object get(@Untainted Object o) {
    return o;
  }

  void test() {
    NewClass newClass = new NewClass(get(get("")));
  }
}
