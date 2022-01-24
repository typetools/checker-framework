// @below-java17-jdk-skip-test
// This is a test case for https://github.com/typetools/checker-framework/issues/5013.

public class BindingVariable {
  public static int bar(Object o) {
    if (o instanceof String s) {
      return s.length();
    } else {
      return 0;
    }
  }
}
