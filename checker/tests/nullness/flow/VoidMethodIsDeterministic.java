import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class VoidMethodIsDeterministic {
  @SideEffectFree
  void method() {}

  @Pure
  Object method2() {
    method();
    return "";
  }
}
