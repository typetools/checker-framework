// A test for a new false positive issued in release 3.36.0 but not 3.35.0.
// Reported as part of https://github.com/typetools/checker-framework/issues/6077.

import java.io.InputStream;
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
      // TODO: a required.method.not.called error should be issued on this line, but currently
      // it is not. The reason is an interaction between type variable defaulting,
      // local dataflow, and the rules that the RLC uses for choosing a variable's must-call
      // obligations: local inference defaults literalOption to @MustCallUnknown (i.e., the
      // top MustCall type) even though the RHS expression's type is @MustCall("hashCode").
      // Then, the rule for obligations says that if a variable has the top must-call type,
      // use the type's default must-call type instead. For String, this is @MustCall({}),
      // so no error is issued. This rule is important to avoid false positives in realistic
      // code (such as the first getMode() method in this class).
      String literalOption = indexOptions.get("is_literal");
    } catch (Exception e) {
    }

    return null;
  }

  // This copy of getMode() adds an explicit `@MustCall` annotation to the String and to
  // the local variable. This version currently works as expected, unlike getMode2().
  // Since Map#get returns @NotOwning, this reports no error.
  public static Object getMode2a(Map<String, @MustCall("hashCode") String> indexOptions) {
    try {
      @MustCall("hashCode") String literalOption = indexOptions.get("is_literal");
    } catch (Exception e) {
    }

    return null;
  }

  // This copy of getMode() adds an explicit `@MustCall` annotation to the String and removes
  // the try-catch.
  public static Object getMode3(Map<String, @MustCall("hashCode") String> indexOptions) {
    // Since Map#get returns @NotOwning, this reports no error.
    String literalOption = indexOptions.get("is_literal");
    return null;
  }

  // This copy of getMode() adds an explicit `@MustCall` annotation to the String, removes
  // the try-catch, and makes the return type void.
  public static void getMode4(Map<String, @MustCall("hashCode") String> indexOptions) {
    // Since Map#get returns @NotOwning, this reports no error.
    String literalOption = indexOptions.get("is_literal");
  }

  // This copy of getMode() removes the try-catch and makes the return type void.
  public static void getMode5(Map<String, String> indexOptions) {
    String literalOption = indexOptions.get("is_literal");
  }

  // This variant uses an InputStream (which has a MustCall type by default) as the
  // value type in the map. This is not an error anymore, as the values are permitted
  // to have any @MustCall type.
  public static Object getModeIS(Map<String, InputStream> indexOptions) {
    try {
      // Since Map#get returns @NotOwning, this reports no error.
      InputStream literalOption = indexOptions.get("is_literal");
    } catch (Exception e) {
    }

    return null;
  }
}
