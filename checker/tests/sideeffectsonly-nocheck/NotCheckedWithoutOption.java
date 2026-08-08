// Verifying a `@SideEffectsOnly` annotation against a method body happens only under
// `-AcheckPurityAnnotations`, like every other purity check.  This test runs with
// `-AsuggestPureMethods` but *without* `-AcheckPurityAnnotations`, because issuing a purity
// suggestion does not require the latter option and must not enable body checking as a side
// effect.
//
// Every method below would produce an error if it were checked; the test expects none.

import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class NotCheckedWithoutOption {

  static Collection<Integer> staticColl;

  // Would report `purity.incorrect.sideeffectsonly`:  `staticColl` is not reachable from `this`.
  @SideEffectsOnly("this")
  void modifiesMoreThanListed() {
    staticColl.add(1);
  }

  // Would report `purity.incorrect.sideeffectsonly`:  `#2` is not listed.
  @SideEffectsOnly("#1")
  void modifiesAnUnlistedParameter(Collection<Integer> a, Collection<Integer> b) {
    a.add(1);
    b.add(2);
  }

  // Would report `purity.unknown.sideeffectsonly`:  the callee promises nothing.
  @SideEffectsOnly("#1")
  void callsAnUnannotatedMethod(Collection<Integer> a) {
    unannotated(a);
  }

  void unannotated(Collection<Integer> a) {
    a.add(1);
  }

  // Would report `purity.empty.sideeffectsonly`.
  @SideEffectsOnly({})
  void emptyAnnotation(Collection<Integer> a) {
    a.add(1);
  }

  // Would report `flowexpr.parse.error`.
  @SideEffectsOnly("#1.noSuchMethod()")
  void unparseableAnnotation(Collection<Integer> a) {
    a.add(1);
  }
}
