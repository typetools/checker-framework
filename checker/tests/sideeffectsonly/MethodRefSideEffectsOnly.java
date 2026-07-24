// A method reference must satisfy the `@SideEffectsOnly` annotation of the functional interface
// method it implements. That check reports `purity.methodref` rather than `purity.overriding`.

import java.util.Collection;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class MethodRefSideEffectsOnly {

  interface Mutator {
    @SideEffectsOnly("#1")
    void apply(Collection<Integer> c);
  }

  @SideEffectsOnly("#1")
  static void mutatesArgument(Collection<Integer> c) {
    c.add(1);
  }

  @SideEffectFree
  static void mutatesNothing(Collection<Integer> c) {}

  static void unannotated(Collection<Integer> c) {}

  void ok() {
    Mutator m1 = MethodRefSideEffectsOnly::mutatesArgument;
    // `@SideEffectFree` is a stronger guarantee than any `@SideEffectsOnly`.
    Mutator m2 = MethodRefSideEffectsOnly::mutatesNothing;
  }

  void notOk() {
    // The referenced method promises nothing, so it cannot implement a `@SideEffectsOnly` method.
    // :: error: (purity.methodref)
    Mutator m = MethodRefSideEffectsOnly::unannotated;
  }

  interface DeterministicMutator {
    @Deterministic
    @SideEffectsOnly("#1")
    int apply(Collection<Integer> c);
  }

  @SideEffectsOnly("#1")
  static int mutatesArgumentNondeterministically(Collection<Integer> c) {
    c.add(1);
    return 0;
  }

  void determinismNotOk() {
    // The referenced method satisfies the `@SideEffectsOnly` annotation, but not the
    // `@Deterministic` one.  (Unlike an overriding method, a referenced method does not inherit
    // the annotations of the method that it implements.)
    // :: error: (purity.methodref)
    DeterministicMutator m = MethodRefSideEffectsOnly::mutatesArgumentNondeterministically;
  }
}
