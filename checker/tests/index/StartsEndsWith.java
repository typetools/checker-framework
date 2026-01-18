// Tests string length refinement after startsWith or endsWith return true
// https://github.com/kelloggm/checker-framework/issues/56

import org.checkerframework.common.value.qual.MinLen;

public class StartsEndsWith {

  final String prefix;

  StartsEndsWith(String prefix) {
    this.prefix = prefix;
  }

  String expectedError(String methodName) {
    // :: error: (argument)
    return methodName.substring(prefix.length());
  }

  String propertyName(String methodName) {
    if (methodName.startsWith(prefix)) {
      return methodName.substring(prefix.length());
    } else {
      // :: error: (argument)
      return methodName.substring(prefix.length());
    }
  }

  String propertyNameNot(String methodName) {
    if (!methodName.startsWith(prefix)) {
      // :: error: (argument)
      return methodName.substring(prefix.length());
    } else {
      String result = methodName.substring(prefix.length());
      return result;
    }
  }

  // This particular test is here rather than in the framework tests because it depends on purity
  // annotations for these particular JDK methods.
  static void refineStartsConditional(String str, String prefix) {
    if (prefix.length() > 10 && str.startsWith(prefix)) {
      @MinLen(11) String s11 = str;
    }
  }

  String removeSuffix(String methodName, String suffix) {
    if (methodName.endsWith(suffix)) {
      // TODO: Refinement establishes suffix.length() <= methodName.length(), but
      // the checker cannot (yet) verify the complex arithmetic in substring(0, length - length).
      // :: error: (argument)
      String result = methodName.substring(0, methodName.length() - suffix.length());
      return result;
    } else {
      return null;
    }
  }
}

class StartsEndsWithExternal {
  public static final String staticFinalField = "str";
}
