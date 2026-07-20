// `@SideEffectsOnly` may be written on a constructor.  Assigning a field of the object under
// construction is not a side effect, so such a field need not be listed in the annotation.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ConstructorSideEffectsOnly2 {

  int f;
  List<Integer> list = new ArrayList<>();
  static List<Integer> staticList = new ArrayList<>();

  // Assignments to the new object's own fields are permitted, even though the annotation does not
  // mention `this`.
  @SideEffectsOnly("#1")
  ConstructorSideEffectsOnly(List<Integer> arg) {
    this.f = 1;
    // An unqualified field name is also a field of the object under construction.
    list = new ArrayList<>();
    arg.add(1);
  }

  // Side effects other than on the new object are checked as in any other method.
  @SideEffectsOnly("#1")
  ConstructorSideEffectsOnly(List<Integer> arg, List<Integer> notListed) {
    f = 1;
    // :: error: (purity.incorrect.sideeffectsonly)
    notListed.add(1);
    // :: error: (purity.incorrect.sideeffectsonly)
    staticList.add(1);
  }

  // The exemption is only for the object under construction, not for other objects of the same
  // class.
  @SideEffectsOnly("#2")
  ConstructorSideEffectsOnly(ConstructorSideEffectsOnly other, List<Integer> listed) {
    // :: error: (purity.incorrect.sideeffectsonly)
    other.f = 1;
    listed.add(1);
  }

  // Outside a constructor, assigning a field of `this` is a side effect that must be listed.
  @SideEffectsOnly("this")
  void assignsOwnField() {
    f = 2;
  }

  @SideEffectsOnly("#1")
  void assignsOwnFieldNotListed(List<Integer> arg) {
    // :: error: (purity.incorrect.sideeffectsonly)
    f = 2;
  }
}
