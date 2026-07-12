package sideeffectsonly;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsTest {
  @Tainted Object x;

  void test() {
    method(x);
    method1(x);
    // :: error: argument
    method2(x);
  }

  @EnsuresQualifier(expression = "#1", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void method(Object x) {}

  void method1(@Untainted Object x) {}

  void method2(@Untainted Object x) {}
}
