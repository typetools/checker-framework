// A `@SideEffectsOnly` expression that is not deterministic is rejected at a lambda that
// implements the annotated method, just as it is at the method's declaration.  Otherwise nothing in
// the lambda's body could ever be recognized as the expression, and every modification would be
// reported.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class LambdaNondeterministicSideEffectsOnly {

  static class Holder {
    List<String> list = new ArrayList<>();

    List<String> getList() {
      return list;
    }
  }

  interface Mutator {
    // `#1.getList()` may denote a different list each time it is evaluated.
    @SideEffectsOnly("#1.getList()")
    // :: error: (purity.nondeterministic.sideeffectsonly)
    void mutate(Holder h);
  }

  void useLambda() {
    // The interface method's annotation is rejected here too.  Its declaration may be in a stub
    // file or in another compilation unit, where the error above would not be issued.
    // :: error: (purity.nondeterministic.sideeffectsonly)
    Mutator m = h -> h.getList().add("x");
  }
}
