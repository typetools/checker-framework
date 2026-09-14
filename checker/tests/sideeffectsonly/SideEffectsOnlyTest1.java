import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsOnlyTest1 {
  @Tainted Object x;

  void test0() {
    method(x);
    method1(x);
    // The field "this.x" may be modified by a method that side-effects "this".
    // :: error: assignment
    @Untainted Object y = x;
  }

  void test() {
    method(x);
    method2(x);
    // `method2()` is specified to side-effect its argument.
    // :: error: assignment
    @Untainted Object y = x;
  }

  @EnsuresQualifier(expression = "#1", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void method(Object x) {}

  @SideEffectsOnly({"this"})
  void method1(@Untainted Object x) {}

  @SideEffectsOnly({"#1"})
  void method2(@Untainted Object x) {}
}
