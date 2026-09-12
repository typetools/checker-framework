// A `@SideEffectsOnly` expression that cannot be parsed is reported at the declaration and at
// every call site, and can be suppressed at either place.  A call site is also reported because
// the callee's declaration might not be under compilation.
//
// This test cannot check that the error is reported only once per call site, because the test
// framework discards duplicate diagnostics.  checker/jtreg/sideeffectsonly/ checks that.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SideEffectsOnlyParseError {

  @SideEffectsOnly("nosuchfield")
  // :: error: (flowexpr.parse.error.sideeffectsonly)
  void modifies() {}

  void call() {
    // :: error: (flowexpr.parse.error.sideeffectsonly)
    modifies();
  }

  void callInLoop(int n) {
    for (int i = 0; i < n; i++) {
      // :: error: (flowexpr.parse.error.sideeffectsonly)
      modifies();
    }
  }

  @SuppressWarnings("flowexpr.parse.error")
  void suppressed() {
    modifies();
  }

  @SideEffectsOnly("alsonosuchfield")
  @SuppressWarnings("flowexpr.parse.error")
  void suppressedAtDeclaration() {}
}
