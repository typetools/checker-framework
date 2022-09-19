// Based on a snippet of code that caused a malformed ajava file to be
// produced by WPI. The offending ajava file contained an @SameLen annotation
// whose argument was a constant String, e.g., @SameLen({ ""Hamburg"", "word1" })

public class ContainsSubstring {

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
}
