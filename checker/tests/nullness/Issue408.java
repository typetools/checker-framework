// Test case for Issue 408
// https://github.com/typetools/checker-framework/issues/408

import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class Issue408 {
  static class Bar {
    Bar() {
      doIssue408();
    }

    String doIssue408(@UnderInitialization Bar this) {
      return "";
    }
  }

  static class Baz extends Bar {
    String myString = "hello";

    @Override
    String doIssue408(@UnderInitialization Baz this) {
      // :: error: (dereference.of.nullable)
      return myString.toLowerCase();
    }
  }

  public static void main(String[] args) {
    new Baz();
  }
}
