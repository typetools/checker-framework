// `@SideEffectsOnly`'s @Target permits CONSTRUCTOR. On a constructor, the annotation constrains
// what the constructor modifies besides the object being constructed.

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ConstructorSideEffectsOnly {

  Object f;
  static Collection<Integer> staticColl;

  // Assigning to the new object's own fields is covered by "this".
  @SideEffectsOnly("this")
  ConstructorSideEffectsOnly() {
    f = null;
    this.f = null;
  }

  // A constructor may be annotated to permit modifying one of its arguments.
  @SideEffectsOnly({"this", "#1"})
  ConstructorSideEffectsOnly(Collection<Integer> c) {
    f = null;
    c.add(1);
  }

  // Modifying state that the annotation does not list is an error, just as in a method.
  @SideEffectsOnly("this")
  ConstructorSideEffectsOnly(int unused) {
    // :: error: (purity.incorrect.sideeffectsonly)
    staticColl.add(1);
  }

  @SideEffectsOnly("this")
  ConstructorSideEffectsOnly(Collection<Integer> c, int unused) {
    // `c` is not listed in the annotation.
    // :: error: (purity.incorrect.sideeffectsonly)
    c.add(1);
  }
}
