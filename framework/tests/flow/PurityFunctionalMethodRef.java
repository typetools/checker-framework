// A method reference is checked like an override: a call through the functional method must check
// the arguments to the referenced method's functional-interface parameters at least as strictly as
// a call to the referenced method does.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalMethodRef {

  @SideEffectFree
  static int applyStatic(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @SideEffectFree
  int applyInstance(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  interface Applier {
    @SideEffectFree
    int apply(Function<String, Integer> f, String s);
  }

  interface UnboundApplier {
    @SideEffectFree
    int apply(PurityFunctionalMethodRef receiver, Function<String, Integer> f, String s);
  }

  interface GenericApplier<T> {
    @SideEffectFree
    int apply(T f, String s);
  }

  interface UnannotatedApplier {
    int apply(Function<String, Integer> f, String s);
  }

  void references() {
    Applier a = PurityFunctionalMethodRef::applyStatic;
    Applier b = this::applyInstance;
    UnboundApplier c = PurityFunctionalMethodRef::applyInstance;
    // :: error: [purity.methodref.functional.parameter]
    GenericApplier<Function<String, Integer>> d = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier e = PurityFunctionalMethodRef::applyStatic;
  }
}
