// A simple test that the @AinferTreatAsSibling1 annotation can be inferred.
// This test does actually test inference: the AinferTestChecker's TreeAnnotator
// has logic to add the @AinferTreatAsSibling1 annotation to parameters with
// the name "iShouldBeTreatedAsSibling1".

import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

public class TreatAsSibling1InferenceTest {
  public void test(Object iShouldBeTreatedAsSibling1) {
    // :: warning: (assignment)
    @AinferSibling1 Object x = iShouldBeTreatedAsSibling1;
  }
}
