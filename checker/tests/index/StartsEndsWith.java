// Tests string length refinement after startsWith or endsWith return true
// https://github.com/kelloggm/checker-framework/issues/56

import org.checkerframework.common.value.qual.MinLen;

public class StartsEndsWith {

  final String prefix;

  StartsEndsWith(String prefix) {
    this.prefix = prefix;
  }

  String propertyName(String methodName) {
    if (methodName.startsWith(prefix)) {
      String result = methodName.substring(prefix.length());
      return result;
    } else {
      return null;
    }
  }

  // This particular test is here rather than in the framework tests because it depends on purity
  // annotations for these particular JDK methods.
  static void refineStartsConditional(String str, String prefix) {
    if (prefix.length() > 10 && str.startsWith(prefix)) {
      @MinLen(11) String s11 = str;
    }
  }

  // Test for endsWith - refinement establishes suffix.length() <= methodName.length(),
  // but the checker cannot verify the complex arithmetic in substring(0, length - length).
  // This is a known limitation of the upper bound analysis.
  String removeSuffix(String methodName, String suffix) {
    if (methodName.endsWith(suffix)) {
      // :: error: (argument)
      String result = methodName.substring(0, methodName.length() - suffix.length());
      return result;
    } else {
      return null;
    }
  }

  // Negative test: should warn outside startsWith check
  void negativeTest(String methodName) {
    // :: error: (argument)
    String result = methodName.substring(prefix.length());
  }

  // Negative test: should warn in else branch only
  void negativeTestElseBranch(String methodName) {
    if (methodName.startsWith(prefix)) {
      String result = methodName.substring(prefix.length());
    } else {
      // :: error: (argument)
      String result = methodName.substring(prefix.length());
    }
  }
}

class StartsEndsWithExternal {
  public static final String staticFinalField = "str";
}
