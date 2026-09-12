// The override check compares the annotations' arguments as Java expressions rather than as
// strings, so an override may list an expression that is reached through an expression that the
// overridden method lists.

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class OverrideExpressionComparison {

  static class Holder {
    Collection<Integer> f;
    Collection<Integer> g;
  }

  interface Super {
    @SideEffectsOnly("#1")
    void m(Holder h);
  }

  // `@SideEffectsOnly("#1.f")` permits strictly less than `@SideEffectsOnly("#1")` does, because
  // `#1.f` is reached through `#1`.
  static class NarrowsToASubexpression implements Super {
    @Override
    @SideEffectsOnly("#1.f")
    public void m(Holder h) {
      h.f.add(1);
    }
  }

  // The parameter's name in the overriding method is irrelevant; `#1` denotes the first parameter
  // of whichever method the annotation is written on.
  static class RenamedParameter implements Super {
    @Override
    @SideEffectsOnly("#1.f")
    public void m(Holder renamed) {
      renamed.f.add(1);
    }
  }

  // An expression that is not reached through an expression that the overridden method lists is
  // still an error.
  static class WidensToAnotherExpression implements Super {
    Collection<Integer> coll;

    @Override
    @SideEffectsOnly({"#1.f", "this.coll"})
    // :: error: (purity.sideeffectsonly.overriding)
    public void m(Holder h) {
      h.f.add(1);
      coll.add(1);
    }
  }

  interface SuperThis {
    @SideEffectsOnly("this")
    void m();
  }

  // `this.f` is reached through `this`, even though the two annotations' strings differ.
  static class NarrowsToAField implements SuperThis {
    Collection<Integer> f;

    @Override
    @SideEffectsOnly("this.f")
    public void m() {
      f.add(1);
    }
  }

  // A field of another object is not reached through `this`.
  static class WidensToAnotherObject implements SuperThis {
    static Holder staticHolder;

    @Override
    @SideEffectsOnly("staticHolder.f")
    // :: error: (purity.sideeffectsonly.overriding)
    public void m() {
      staticHolder.f.add(1);
    }
  }
}
