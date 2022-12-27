// Test case for https://github.com/typetools/checker-framework/issues/5471.
// @skip-test until the issue is fixed.

import org.checkerframework.checker.index.qual.IndexFor;

public class Issue5471 {
  private static boolean atTheBeginning(@IndexFor("#2") int index, String line) {
    return (index==0);
  }

  private static boolean hasDoubleQuestionMarkAtTheBeginning(String line) {
    int i = line.indexOf("??");
    if (i != -1) {
      // Unexpected warning on this line: i is definitely an index for line.
      return (atTheBeginning(i, line));
    }
    return false;
  }

  public static void main(String[] args) {
    String x = "Hello?World, this is our new program";
    if (hasDoubleQuestionMarkAtTheBeginning(x))
      System.out.println("TRUE");
  }
}
