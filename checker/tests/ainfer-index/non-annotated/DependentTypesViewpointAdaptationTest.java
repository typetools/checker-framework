// Based on a snippet of code that caused a malformed ajava file to be
// produced by WPI. The offending ajava file contained an @SameLen annotation
// whose argument was a constant String, e.g., @SameLen({ ""Hamburg"", "word1" })

import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.qual.NonNegative;

public class DependentTypesViewpointAdaptationTest {

  // These taken from https://checkerframework.org/manual/#index-annotating-fixed-size
  // to make this class a valid target for SameLen.
  private final Object @SameLen("this") [] delegate;

  @SuppressWarnings("index") // constructor creates object of size @SameLen(this) by definition
  DependentTypesViewpointAdaptationTest(@NonNegative int size) {
    delegate = new Object[size];
  }

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

  public static void thisTest(@SameLen("#2") DependentTypesViewpointAdaptationTest t1,
      @SameLen("#1") DependentTypesViewpointAdaptationTest t2) {
    t1.compute2(t2);
  }

  public boolean compute2(DependentTypesViewpointAdaptationTest this,
      DependentTypesViewpointAdaptationTest other) {
    // content doesn't matter
    return false;
  }
}
