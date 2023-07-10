// A test for a new false positive issued in release 3.36.0 but not 3.35.0.
// Reported as part of https://github.com/typetools/checker-framework/issues/6077.

import java.util.Map;
import org.checkerframework.checker.mustcall.qual.MustCall;

public class IndexMode {
  public static Object getMode(Map<String, String> indexOptions) {
    // Try-catch is needed, otherwise no FP.
    try {
      String literalOption = indexOptions.get("is_literal");
    } catch (Exception e) {
    }

    // Actual return type rather than void is needed, otherwise no FP.
    return null;
  }

  // This copy of getMode() adds an explicit `@MustCall` annotation to the String.
  public static Object getMode2(Map<String, @MustCall("hashCode") String> indexOptions) {
    try {
      // :: error: required.method.not.called
      String literalOption = indexOptions.get("is_literal");
    } catch (Exception e) {
    }

    return null;
  }

  // This copy of getMode() adds an explicit `@MustCall` annotation to the String and removes
  // the try-catch.
  public static Object getMode3(Map<String, @MustCall("hashCode") String> indexOptions) {
    // :: error: required.method.not.called
    String literalOption = indexOptions.get("is_literal");
    return null;
  }

  // This copy of getMode() adds an explicit `@MustCall` annotation to the String, removes
  // the try-catch, and makes the return type void.
  public static void getMode4(Map<String, @MustCall("hashCode") String> indexOptions) {
    // :: error: required.method.not.called
    String literalOption = indexOptions.get("is_literal");
  }
}
