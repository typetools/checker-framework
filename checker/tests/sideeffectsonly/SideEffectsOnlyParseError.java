// A `@SideEffectsOnly` expression that cannot be parsed at a call site is reported at the call
// site, and can be suppressed there.
//
// This test cannot check that the error is reported only once per call site, because the test
// framework discards duplicate diagnostics.  checker/jtreg/sideeffectsonly/ checks that.

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
