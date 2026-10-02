// The requirement on a functional-interface argument applies to the annotated JDK's methods, which
// is where most calls to a side-effect-free method with a functional-interface parameter occur.
//
// The requirement holds at every call site, whatever the purity of the method that contains it.

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class PurityFunctionalArgumentJdk {

  int count = 0;

  // Optional.map is @SideEffectFree:  "the mapper must not have side effects".

  void optionalMap(Optional<String> o) {
    o.map(s -> s.length());
    // :: error: [purity.assign.field]
    o.map(s -> count++);
  }

  // Collections.min is @Pure and List.sort is @SideEffectsOnly("this"), so each requires a
  // side-effect-free comparator.  Comparator.compare is @Pure, so a value of that type has one.

  void comparatorArguments(Collection<String> c, List<String> l, Comparator<String> comparator) {
    java.util.Collections.min(c, comparator);
    l.sort(comparator);
  }

  /**
   * A value whose functional method promises no purity cannot be passed: Function.apply is not
   * annotated.
   */
  void unannotatedFunctionalValue(Optional<String> o, Function<String, Integer> f) {
    // :: error: [purity.functional.argument]
    o.map(f);
  }

  /**
   * A comparator written at the call site is checked directly, against the @Pure on
   * Comparator.compare.
   */
  void comparatorWrittenHere(List<String> l) {
    l.sort((a, b) -> a.length() - b.length());
    // :: error: [purity.assign.field]
    l.sort((a, b) -> count++);
  }

  /** An unannotated JDK method imposes nothing: Stream.map is not annotated. */
  void unannotatedJdkMethod(Stream<String> s) {
    s.map(t -> count++);
  }
}
