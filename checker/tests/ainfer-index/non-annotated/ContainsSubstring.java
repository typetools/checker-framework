// Based on a snippet of code that caused a malformed ajava file to be
// produced by WPI. The offending ajava file contained an @SameLen annotation
// whose argument was a constant String, e.g., @SameLen({ ""Hamburg"", "word1" })

public class ContainsSubstring {

  @SuppressWarnings("samelen") // TODO: caused by a bug related to viewpoint adaptation. Make WPI
  // actually viewpoint-adapt the annotations that it infers, then
  // remove this warning suppression.
  public static void run() {
    String word1 = "\"Hamburg\"";
    String word2 = "burg";
    System.out.println(compute(word1, word2));
  }

  public static boolean compute(String word1, String word2) {
    // content doesn't matter
    return false;
  }
}
