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
    System.out.println(compute(word1, word2));
  }

  public static boolean compute(String word1, String otherWord) {
    // content doesn't matter
    return false;
  }

  public static void receiverTest(@SameLen("#2") DependentTypesViewpointAdaptationTest t1,
      @SameLen("#1") DependentTypesViewpointAdaptationTest t2) {
    t1.compute2(t2);
  }

  public boolean compute2(DependentTypesViewpointAdaptationTest this,
      DependentTypesViewpointAdaptationTest other) {
    // content doesn't matter
    return false;
  }

  public void thisTest(@SameLen("this") DependentTypesViewpointAdaptationTest t1) {
    compute3(this, t1);
  }

  public static boolean compute3(DependentTypesViewpointAdaptationTest t1,
      DependentTypesViewpointAdaptationTest t2) {
    // content doesn't matter
    return false;
  }

  public void thisTestNoUse(@SameLen("this") DependentTypesViewpointAdaptationTest t1) {
    compute4(t1, t1);
  }

  public static boolean compute4(DependentTypesViewpointAdaptationTest t1,
      DependentTypesViewpointAdaptationTest t2) {
    // content doesn't matter
    return false;
  }
}
