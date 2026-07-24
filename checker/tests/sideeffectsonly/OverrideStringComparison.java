// The override check compares the annotations' string arguments rather than the Java expressions
// that they stand for.  That comparison is conservative:  it reports an error where none is
// warranted.  This file pins down the known false positive, so that fixing the TODO in
// `BaseTypeVisitor.checkPurity` is a deliberate, visible change.

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class OverrideStringComparison {

  static class Holder {
    Collection<Integer> f;
  }

  interface Super {
    @SideEffectsOnly("#1")
    void m(Holder h);
  }

  // `@SideEffectsOnly("#1.f")` permits strictly less than `@SideEffectsOnly("#1")` does, because
  // `#1.f` is reached through `#1`.  Comparing the strings "#1.f" and "#1" does not recognize
  // that, so an error is reported even though the override is sound.
  static class NarrowsToASubexpression implements Super {
    @Override
    @SideEffectsOnly("#1.f")
    // :: error: (purity.overriding)
    public void m(Holder h) {
      h.f.add(1);
    }
  }
}
