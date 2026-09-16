// The requirement on a functional-interface argument applies to the annotated JDK's methods, which
// is where most calls to a side-effect-free method with a functional-interface parameter occur.
//
// The requirement holds at every call site, whatever the purity of the method that contains it.

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.checkerframework.dataflow.qual.Pure;

public class PurityFunctionalArgumentJdk {

  int count = 0;

  /**
   * Collections.min is @Pure, so a comparator passed to it must be @Pure, not merely pure of side
   * effects.
   */
  @FunctionalInterface
  interface PureComparator<T> extends Comparator<T> {
    @Override
    @Pure
    int compare(T a, T b);
  }

  // Optional.map is @SideEffectFree:  "the mapper must not have side effects".

  void optionalMap(Optional<String> o) {
    o.map(s -> s.length());
    // :: error: [purity.not.sideeffectfree.assign.field]
    o.map(s -> count++);
  }

  // Collections.min is @Pure and List.sort is @SideEffectsOnly("this"), so each requires a
  // side-effect-free comparator.  Comparator.compare is not annotated, so a value of that type
  // does not have one.

  void comparatorArguments(Collection<String> c, List<String> l, Comparator<String> comparator) {
    // :: error: [purity.functional.argument]
    java.util.Collections.min(c, comparator);
    // :: error: [purity.functional.argument]
    l.sort(comparator);
  }

  /** Declaring the purity in the comparator's own type is what makes such a value passable. */
  void pureComparatorArguments(Collection<String> c, List<String> l, PureComparator<String> pure) {
    java.util.Collections.min(c, pure);
    l.sort(pure);
  }

  /** A comparator written at the call site is checked directly, and this one is pure. */
  void comparatorWrittenHere(List<String> l) {
    l.sort((a, b) -> a.length() - b.length());
    // :: error: [purity.not.sideeffectfree.assign.field]
    l.sort((a, b) -> count++);
  }

  /** An unannotated JDK method imposes nothing: Stream.map is not annotated. */
  void unannotatedJdkMethod(Stream<String> s) {
    s.map(t -> count++);
  }
}
