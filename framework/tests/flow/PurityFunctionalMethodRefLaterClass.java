// A method reference to a method of a top-level class that javac processes later, whose body
// might not be attributed when the method reference is checked.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalMethodRefLaterClass {

  @FunctionalInterface
  interface UnannotatedApplier {
    int apply(Function<String, Integer> f, String s);
  }

  void references() {
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier a = PurityFunctionalMethodRefLater::apply;
    // Overwriting f does not read it, even in a body that has not been attributed.
    UnannotatedApplier b = PurityFunctionalMethodRefLater::overwrite;
    // The index of the array access calls f.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier c = PurityFunctionalMethodRefLater::overwriteArray;
  }
}

class PurityFunctionalMethodRefLater {

  @SideEffectFree
  static int apply(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @SideEffectFree
  static int overwrite(Function<String, Integer> f, String s) {
    f = null;
    return 0;
  }

  @SideEffectFree
  static int overwriteArray(Function<String, Integer> f, String s) {
    int[] a = new int[1];
    // :: error: [purity.assign.array]
    a[f.apply(s)] = 0;
    return 0;
  }
}
