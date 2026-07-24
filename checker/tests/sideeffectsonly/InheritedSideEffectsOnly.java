// `@SideEffectsOnly` is inherited by overriding methods, so an override that does not repeat the
// annotation is nonetheless checked against the overridden method's annotation.

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class InheritedSideEffectsOnly {

  interface Super {
    @SideEffectsOnly("#1")
    void mutateFirst(Collection<Integer> a, Collection<Integer> b);

    @SideEffectsOnly("this")
    void mutateThis();
  }

  // An unannotated override obeys the inherited annotation.
  static class ObeysInherited implements Super {
    int f;

    @Override
    public void mutateFirst(Collection<Integer> a, Collection<Integer> b) {
      a.add(1);
    }

    @Override
    public void mutateThis() {
      f = 1;
    }
  }

  // An unannotated override that exceeds the inherited annotation is an error, even though the
  // override itself carries no annotation.
  static class ViolatesInherited implements Super {
    static Collection<Integer> staticColl;

    @Override
    public void mutateFirst(Collection<Integer> a, Collection<Integer> b) {
      // :: error: (purity.incorrect.sideeffectsonly)
      b.add(1);
    }

    @Override
    public void mutateThis() {
      // A static field is not reachable from `this`.
      // :: error: (purity.incorrect.sideeffectsonly)
      staticColl.add(1);
    }
  }

  // `#1` is view-adapted to the overriding method, so the parameter names need not match.
  static class RenamedParameters implements Super {
    @Override
    public void mutateFirst(Collection<Integer> renamedA, Collection<Integer> renamedB) {
      renamedA.add(1);
      // :: error: (purity.incorrect.sideeffectsonly)
      renamedB.add(1);
    }

    @Override
    public void mutateThis() {}
  }

  // An override may promise more: `@SideEffectFree` is stronger than any `@SideEffectsOnly`.
  static class Strengthens implements Super {
    @Override
    @SideEffectFree
    public void mutateFirst(Collection<Integer> a, Collection<Integer> b) {}

    @Override
    @SideEffectFree
    public void mutateThis() {}
  }

  interface SuperTwo {
    @SideEffectsOnly({"#1", "#2"})
    void mutateBoth(Collection<Integer> a, Collection<Integer> b);
  }

  // An explicit annotation on the override replaces the inherited one, and may list a subset.
  static class NarrowsExplicitly implements SuperTwo {
    @Override
    @SideEffectsOnly("#1")
    public void mutateBoth(Collection<Integer> a, Collection<Integer> b) {
      a.add(1);
      // :: error: (purity.incorrect.sideeffectsonly)
      b.add(1);
    }
  }

  // An explicit annotation on the override may not list more than the overridden method does.
  static class WidensExplicitly implements SuperTwo {
    Collection<Integer> coll;

    @Override
    @SideEffectsOnly({"#1", "#2", "this.coll"})
    // :: error: (purity.overriding)
    public void mutateBoth(Collection<Integer> a, Collection<Integer> b) {
      coll.add(1);
    }
  }
}
