import org.checkerframework.dataflow.qual.SideEffectsOnly;

/**
 * Each of the two {@code @SideEffectsOnly} diagnostics below is issued once per call site, even
 * though dataflow analyzes each call site once per iteration of the loop and every checker that
 * Main.java runs this file through analyzes each call site. The diagnostic about the lambda is
 * likewise issued once, though every checker checks the lambda.
 *
 * <p>The same holds of the diagnostic that {@code unparseable}'s declaration produces. Two checks
 * detect that error -- checking the annotation itself, and parsing the expression in order to check
 * the method body against it -- but they issue the same message, so it is reported only once.
 *
 * <p>The directory test checker/tests/sideeffectsonly/SideEffectsOnlyParseError.java cannot detect
 * a duplicate diagnostic, because the test framework discards duplicates before comparing the
 * diagnostics to the expected ones.
 */
public class SideEffectsOnlyDiagnostics {

  /** The expression cannot be parsed in any scope. */
  @SideEffectsOnly("nosuchfield")
  void unparseable() {}

  /** The expression can be parsed, but not every call site can represent it. */
  @SideEffectsOnly({"this", "#1"})
  void unrepresentable(Object o) {}

  void callInLoop(int n) {
    for (int i = 0; i < n; i++) {
      unparseable();
      unrepresentable(new Object());
    }
  }

  static int field;

  /** A lambda is checked against the annotation on the method that it implements. */
  void lambda() {
    Callback c = () -> field = 1;
  }
}

interface Callback {
  /** The expression cannot be parsed in any scope. */
  @SideEffectsOnly("nosuchfield")
  void run();
}
