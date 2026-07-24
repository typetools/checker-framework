import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsOnlyTest2 {
  @Tainted Object w;
  @Tainted Object x;

  void test0() {
    method(x);
    method1(x);
    method3(x);
    @Untainted Object y = x;
  }

  @EnsuresQualifier(expression = "#1", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void method(Object x) {}

  @SideEffectsOnly({"w"})
  void method1(@Untainted Object x) {}

  @SideEffectsOnly({"w"})
  void method3(@Untainted Object z) {}
}
