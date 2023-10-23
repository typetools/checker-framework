// Based on a snippet of code that caused a malformed ajava file to be
// produced by WPI. The offending ajava file contained an @SameLen annotation
// whose argument was a constant String, e.g., @SameLen({ ""Hamburg"", "word1" })

import org.checkerframework.checker.index.qual.SameLen;

public class DependentTypesViewpointAdaptationTest {

  public static void run() {
    String word1 = "\"Hamburg\"";
    String word2 = "burg";
    // The existence of word3 here forces the inferred @SameLen
    // annotation to include a local variable that isn't a parameter
    // to compute(), to test that such local variables are viewpoint-adapted
    // correctly.
    String word3 = word1;
    compute(word1, word2);
  }

  public static void compute(String word1, String otherWord) {
    // content doesn't matter
  }

  public static void receiverTest(
      @SameLen("#2") DependentTypesViewpointAdaptationTest t1,
      @SameLen("#1") DependentTypesViewpointAdaptationTest t2) {
    t1.compute2(t2);
  }

  public void compute2(
      DependentTypesViewpointAdaptationTest this, DependentTypesViewpointAdaptationTest other) {
    // content doesn't matter
  }

  public void thisTest(@SameLen("this") DependentTypesViewpointAdaptationTest t1) {
    compute3(this, t1);
  }

  public static void compute3(
      DependentTypesViewpointAdaptationTest t1, DependentTypesViewpointAdaptationTest t2) {
    // content doesn't matter
  }

  public void thisTestNoUse(@SameLen("this") DependentTypesViewpointAdaptationTest t1) {
    compute4(t1, t1);
  }

  public static void compute4(
      DependentTypesViewpointAdaptationTest t1, DependentTypesViewpointAdaptationTest t2) {
    // content doesn't matter
  }

  public static void testThisInference(
      DependentTypesViewpointAdaptationTest t1,
      @SameLen("#1") DependentTypesViewpointAdaptationTest t2) {
    t1.compute5(t2);
    t1.compute6(t2);
  }

  public void compute5(
      DependentTypesViewpointAdaptationTest this, DependentTypesViewpointAdaptationTest other) {
    // :: warning: (assignment)
    @SameLen("this") DependentTypesViewpointAdaptationTest myOther = other;
  }

  // Same as compute5, but without an explicit this parameter.
  public void compute6(DependentTypesViewpointAdaptationTest other) {
    // :: warning: (assignment)
    @SameLen("this") DependentTypesViewpointAdaptationTest myOther = other;
  }
}
