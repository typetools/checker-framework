package sideeffectsonly;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsOnlyTest1 {
  @Tainted Object x;

  void test() {
    method(x);
    method1(x);
    method3(x);
    method2(x);
    // :: error: argument
    method3(x);
  }

  @EnsuresQualifier(expression = "#1", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void method(Object x) {}

  @SideEffectsOnly({"this"})
  void method1(@Untainted Object x) {}

  @SideEffectsOnly({"#1"})
  void method2(@Untainted Object x) {}

  @SideEffectsOnly({"this"})
  void method3(@Untainted Object z) {}
}
