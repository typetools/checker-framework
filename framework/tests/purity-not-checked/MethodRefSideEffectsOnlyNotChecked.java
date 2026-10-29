// Without -AcheckPurityAnnotations, a method reference is not checked against the
// `@SideEffectsOnly` annotation of the functional interface method, so no diagnostic is issued.
// This file holds violations that checker/tests/sideeffectsonly/MethodRefSideEffectsOnly.java
// asserts.

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class MethodRefSideEffectsOnlyNotChecked {

  interface Mutator {
    @SideEffectsOnly("#1")
    void apply(Collection<Integer> c);
  }

  static void unannotated(Collection<Integer> c) {}

  static class Cell {
    Collection<Integer> coll;

    @SideEffectsOnly("this.coll")
    void mutatesOwnField(Collection<Integer> unused) {}
  }

  interface CellMutator {
    @SideEffectsOnly("#2")
    void apply(Cell cell, Collection<Integer> c);
  }

  void notChecked() {
    Mutator m = MethodRefSideEffectsOnlyNotChecked::unannotated;
    CellMutator cm = Cell::mutatesOwnField;
  }
}
