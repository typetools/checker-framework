// A test for a new false positive issued in release 3.36.0 but not 3.35.0.
// Reported as part of https://github.com/typetools/checker-framework/issues/6077.

import java.util.Map;

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
}
