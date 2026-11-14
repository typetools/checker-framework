// The override check compares the annotations' arguments as Java expressions rather than as
// strings.  An override can list an expression that is reached through an expression that the
// overridden method lists, even if neither string is a prefix of the other.  Conversely, an
// override's expression is not permitted merely because its string equals, or starts with, a string
// that the overridden method lists.

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class OverrideExpressionComparison {

  static class Holder {
    Collection<Integer> f;
    // The name `f` is a prefix of this field's name.
    Collection<Integer> ff;
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

  // `f` is reached through `this`, because the implicit receiver of `f` is `this`, even though the
  // string "f" does not start with the string "this".
  static class NarrowsToAFieldImplicitly implements SuperThis {
    Collection<Integer> f;

    @Override
    @SideEffectsOnly("f")
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

  interface SuperField {
    @SideEffectsOnly("#1.f")
    void m(Holder h);
  }

  // `#1.ff` is not reached through `#1.f`, even though the string "#1.ff" starts with "#1.f".
  static class WidensToAFieldWithAPrefixName implements SuperField {
    @Override
    @SideEffectsOnly("#1.ff")
    // :: error: (purity.sideeffectsonly.overriding)
    public void m(Holder h) {
      h.ff.add(1);
    }
  }

  static class SuperWithField {
    Collection<Integer> f;

    @SideEffectsOnly("this.f")
    void m() {}
  }

  // The override's `this.f` names the field `SubHidesField.f`, which hides `SuperWithField.f`, so
  // the two annotations are written identically but name different fields.
  static class SubHidesField extends SuperWithField {
    Collection<Integer> f;

    @Override
    @SideEffectsOnly("this.f")
    // :: error: (purity.sideeffectsonly.overriding)
    void m() {
      f.add(1);
    }
  }
}
