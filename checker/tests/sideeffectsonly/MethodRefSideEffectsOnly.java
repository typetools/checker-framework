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

  // For an unbound method reference (`Type::instanceMethod`), the interface method's first
  // parameter is the referenced method's receiver, and the interface method's parameter `#(i+1)`
  // is the referenced method's parameter `#i`.  The two annotations must be compared with that
  // shift.

  void unboundOk() {
    // `Collection.clear()` is `@SideEffectsOnly("this")` in the annotated JDK.  Its receiver is
    // `Mutator.apply`'s `#1`, which `Mutator.apply`'s annotation lists.
    Mutator m = Collection<Integer>::clear;
  }

  static class Cell {
    Collection<Integer> coll = new java.util.ArrayList<>();

    @SideEffectsOnly("this.coll")
    void mutatesOwnField(Collection<Integer> unused) {
      coll.add(1);
    }

    @SideEffectsOnly("#1")
    void mutatesArgument(Collection<Integer> c) {
      c.add(1);
    }
  }

  interface CellMutator {
    @SideEffectsOnly("#2")
    void apply(Cell cell, Collection<Integer> c);
  }

  void unboundShiftOk() {
    // `#1` of `mutatesArgument` is `#2` of `CellMutator.apply`, which the interface method's
    // annotation lists.
    CellMutator m = Cell::mutatesArgument;
  }

  void unboundShiftNotOk() {
    // `this.coll` of `mutatesOwnField` is `#1.coll` of `CellMutator.apply`, which
    // `@SideEffectsOnly("#2")` does not cover.
    // :: error: (purity.methodref)
    CellMutator m = Cell::mutatesOwnField;
  }

  void boundNotOk() {
    Cell cell = new Cell();
    // For a bound reference, the receiver is fixed by the reference itself, so no expression at
    // the interface method's declaration denotes what `mutatesOwnField` modifies.
    // :: error: (purity.methodref)
    Mutator m = cell::mutatesOwnField;
  }
}
