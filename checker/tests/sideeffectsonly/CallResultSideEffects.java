// Whether the result of a method call is covered by a `@SideEffectsOnly` annotation that lists
// the call's receiver.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class CallResultSideEffects {

  // `List.subList` is `@SideEffectFree`, so `a.subList(0, 1)` is assumed to be part of `a` and is
  // reached through it.  Modifying it is therefore permitted by `@SideEffectsOnly("#1")`.
  @SideEffectsOnly("#1")
  void modifiesResultOfPureCall(List<String> a) {
    a.subList(0, 1).add("x");
  }

  static List<String> global = new ArrayList<>();

  // A method that is not side-effect-free may return an object that its receiver does not reach,
  // as this one returns a static field.
  @SideEffectsOnly("this")
  List<String> getGlobal() {
    return global;
  }

  // The result of a call that is not side-effect-free is not reached through the call's receiver,
  // so modifying it is not permitted by an annotation that lists only the receiver.
  @SideEffectsOnly("this")
  void modifiesResultOfImpureCall() {
    // :: error: (purity.incorrect.sideeffectsonly)
    getGlobal().add("x");
  }
}
