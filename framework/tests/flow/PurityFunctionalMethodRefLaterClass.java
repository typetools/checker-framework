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
  }
}

class PurityFunctionalMethodRefLater {

  @SideEffectFree
  static int apply(Function<String, Integer> f, String s) {
    return f.apply(s);
  }
}
