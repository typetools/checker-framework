// A call through any method that a lambda's functional interface inherits can run the lambda, so
// the lambda's body must satisfy the `@SideEffectsOnly` annotation of each such method, not only of
// the functional interface method.  Each group of interfaces lists its superinterfaces in a
// different order, so that whichever superinterface's method javac chooses as the functional
// interface method, some test case has the `@SideEffectsOnly` annotation on a method that is not
// the functional interface method.

import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class LambdaInheritedSideEffectsOnly {

  interface Mutator {
    @SideEffectsOnly("#1")
    void apply(Collection<Integer> c);
  }

  interface PlainMutator {
    void apply(Collection<Integer> c);
  }

  interface PlainFirstMutator extends PlainMutator, Mutator {}

  interface PlainLastMutator extends Mutator, PlainMutator {}

  static Collection<Integer> other = new ArrayList<>();

  void ok() {
    PlainFirstMutator m1 = c -> c.add(1);
    PlainLastMutator m2 = c -> c.add(1);
    Object m3 = (PlainMutator & Mutator) c -> c.add(1);
    Object m4 = (Mutator & PlainMutator) c -> c.add(1);
  }

  void notOk() {
    // :: error: (purity.incorrect.sideeffectsonly)
    PlainFirstMutator m1 = c -> other.add(1);
    // :: error: (purity.incorrect.sideeffectsonly)
    PlainLastMutator m2 = c -> other.add(1);
    // :: error: (purity.incorrect.sideeffectsonly)
    Object m3 = (PlainMutator & Mutator) c -> other.add(1);
    // :: error: (purity.incorrect.sideeffectsonly)
    Object m4 = (Mutator & PlainMutator) c -> other.add(1);
  }

  interface OtherMutator {
    @SideEffectsOnly("LambdaInheritedSideEffectsOnly.other")
    void apply(Collection<Integer> c);
  }

  interface BothMutator extends Mutator, OtherMutator {}

  void both() {
    // The body must satisfy both annotations, so it can side-effect neither `#1` nor `other`.
    BothMutator m1 = c -> {};
    // :: error: (purity.incorrect.sideeffectsonly)
    BothMutator m2 = c -> c.add(1);
    // :: error: (purity.incorrect.sideeffectsonly)
    BothMutator m3 = c -> other.add(1);
  }
}
