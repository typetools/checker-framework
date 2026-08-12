// A `@SideEffectsOnly` expression that cannot be parsed at a call site is reported at the call
// site, once per call site rather than once per dataflow iteration, and can be suppressed there.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SideEffectsOnlyParseError {

  // No error is issued here: the annotation is not checked at the declaration.
  @SideEffectsOnly("nosuchfield")
  void modifies() {}

  void call() {
    // :: error: (flowexpr.parse.error)
    modifies();
  }

  void callInLoop(int n) {
    for (int i = 0; i < n; i++) {
      // :: error: (flowexpr.parse.error)
      modifies();
    }
  }

  @SuppressWarnings("flowexpr.parse.error")
  void suppressed() {
    modifies();
  }
}
