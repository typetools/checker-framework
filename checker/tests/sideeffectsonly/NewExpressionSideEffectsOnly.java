// A `new` expression is a call, so the invoked constructor's side effects are checked just as an
// invoked method's are.

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class NewExpressionSideEffectsOnly {

  static class Unannotated {
    Unannotated(Collection<Integer> c) {
      c.add(1);
    }
  }

  static class Annotated {
    @SideEffectsOnly({"this", "#1"})
    Annotated(Collection<Integer> c) {
      c.add(1);
    }
  }

  static class OnlyThis {
    @SideEffectsOnly("this")
    OnlyThis() {}
  }

  @SideEffectsOnly("#1")
  void callsUnannotatedConstructor(Collection<Integer> c) {
    // The constructor might modify arbitrary state.
    // :: error: (purity.unknown.sideeffectsonly)
    new Unannotated(c);
  }

  @SideEffectsOnly("#1")
  void callsAnnotatedConstructor(Collection<Integer> c) {
    // The constructor modifies only `c`, which is listed in this method's annotation.
    new Annotated(c);
  }

  @SideEffectsOnly("#1")
  void callsAnnotatedConstructorWithDisallowedArgument(
      Collection<Integer> c, Collection<Integer> d) {
    // :: error: (purity.incorrect.sideeffectsonly)
    new Annotated(d);
  }

  @SideEffectsOnly("#1")
  void constructingIsNotASideEffect(Collection<Integer> c) {
    // `this` in a constructor's annotation is the object being constructed, which did not exist
    // before the call, so modifying it is not visible to the caller.
    new OnlyThis();
  }
}
