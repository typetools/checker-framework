import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityError {
  @SideEffectFree
  void method() {}

  @Pure
  Object method2() {
    // :: error: (purity.not.deterministic.call)
    method();
    return "";
  }
}
