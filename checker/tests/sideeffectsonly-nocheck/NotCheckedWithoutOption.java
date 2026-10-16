// Verifying a `@SideEffectsOnly` annotation against a method body happens only under
// `-AcheckPurityAnnotations`, like every other purity check.  This test runs with
// `-AsuggestPureMethods` but *without* `-AcheckPurityAnnotations`, because issuing a purity
// suggestion does not require the latter option and must not enable body checking as a side
// effect.
//
// Every method below would produce an error if its body were checked; the test expects none,
// except for the one annotation that cannot be parsed.  Dataflow acts on a `@SideEffectsOnly`
// annotation whether or not `-AcheckPurityAnnotations` was supplied, so an unparseable annotation
// is an error in every configuration.

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

  // The syntax of the annotation is checked even without `-AcheckPurityAnnotations`.
  @SideEffectsOnly("#1.noSuchMethod()")
  // :: error: flowexpr.parse.error.sideeffectsonly
  void unparseableAnnotation(Collection<Integer> a) {
    a.add(1);
  }

  // Would report `purity.impure.sideeffectsonly`:  two evaluations of `#1.iterator()`
  // may yield unrelated values.
  @SideEffectsOnly("#1.iterator()")
  void nondeterministicAnnotation(Collection<Integer> a) {
    a.add(1);
  }
}
