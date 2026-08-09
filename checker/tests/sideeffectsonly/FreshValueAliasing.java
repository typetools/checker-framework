// Assigning an expression whose value is computed afresh at each evaluation -- a literal, a method
// call, or an array creation -- does not make two variables that are assigned syntactically
// identical such expressions aliases of one another.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class FreshValueAliasing {

  @SideEffectFree
  static List<String> makeList() {
    return new ArrayList<>();
  }

  // Initializing two variables to `null` relates nothing: `null` is not an object.
  @SideEffectsOnly("#1")
  void nullInitializers(List<String> a, List<String> b) {
    List<String> x = null;
    List<String> y = null;
    x = a;
    y = b;
    // :: error: (purity.incorrect.sideeffectsonly)
    y.add("x");
  }

  // Two calls to the same method return unrelated objects.
  @SideEffectsOnly("#1")
  void methodCallInitializers(List<String> a, List<String> b) {
    List<String> x = makeList();
    List<String> y = makeList();
    x = a;
    y = b;
    // :: error: (purity.incorrect.sideeffectsonly)
    y.add("x");
  }

  // Two array creations of the same size produce unrelated arrays.
  @SideEffectsOnly("#1")
  void arrayCreationInitializers(String[] a, String[] b) {
    String[] x = new String[2];
    String[] y = new String[2];
    x = a;
    y = b;
    // :: error: (purity.incorrect.sideeffectsonly)
    y[0] = "x";
  }

  // An alias that a method call creates is still recognized: `List.subList` is `@SideEffectFree`,
  // so `a.subList(0, 1)` is assumed to be part of `a` and is reached through it.  Modifying it is
  // therefore permitted by `@SideEffectsOnly("#1")`.
  @SideEffectsOnly("#1")
  void aliasThroughMethodCall(List<String> a) {
    List<String> x = a.subList(0, 1);
    x.add("x");
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
