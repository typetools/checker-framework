// Inference of purity for a functional interface method that a method reference implements.  The
// method reference constrains the purity of the functional interface method, just as a lambda or
// an overriding method does.  If inference ignored the method reference, it would infer @Pure for
// `ImpureSupplier.get`, whose declaration has no body, and the validation run would report
// purity.methodref.

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.EnsuresQualifierIf;

public class PurityInferenceMethodRef {

  Object obj;

  int count;

  interface ImpureSupplier {
    Object get();
  }

  Object impure() {
    count++;
    return null;
  }

  void useImpure() {
    ImpureSupplier s = this::impure;
  }

  // A method reference to a pure method does not prevent inference of @Pure for
  // `PureSupplier.get`.
  interface PureSupplier {
    Object get();
  }

  @Pure
  Object pure() {
    return null;
  }

  void usePure() {
    PureSupplier s = this::pure;
  }

  @SuppressWarnings("ainfertest")
  @EnsuresQualifierIf(expression = "#1", result = true, qualifier = AinferSibling1.class)
  boolean checkAinferSibling1(Object o) {
    return true;
  }

  // The call below unrefines the type of `obj` unless `PureSupplier.get` is side-effect-free.
  @AinferSibling1 Object callPureSupplier(PureSupplier s) {
    if (checkAinferSibling1(obj)) {
      s.get();
      // :: warning: [return]
      return obj;
    }
    return null;
  }
}
