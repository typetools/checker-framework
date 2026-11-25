import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * A call through any method that a lambda's functional interface inherits can run the lambda, so
 * the lambda's body must be at least as pure as each such method. Each error message names the
 * methods that require the purity that the body lacks, which need not be the functional interface
 * method. The error about a method body names no method.
 */
public class LambdaPurityRequiredBy {

  interface SefFunc {
    @SideEffectFree
    Object doNothing();
  }

  interface PureFunc {
    @Pure
    Object doNothing();
  }

  interface PlainFunc {
    Object doNothing();
  }

  interface PlainFirstFunc extends PlainFunc, SefFunc {}

  interface SefAndPureFunc extends SefFunc, PureFunc {}

  int count;

  Object impure() {
    count++;
    return null;
  }

  void lambdas() {
    PlainFirstFunc f1 = () -> impure();
    SefAndPureFunc f2 = () -> impure();
    Object f3 = (PlainFunc & SefFunc) () -> impure();
  }

  @SideEffectFree
  void method() {
    count++;
  }
}
