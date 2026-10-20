// The Nullness Checker is a compound checker, and a subchecker's dataflow analysis is what
// computes a `@SideEffectsOnly` diagnostic.  Nonetheless, "nullness" -- the prefix of the checker
// that the user ran -- suppresses the diagnostic.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SideEffectsOnlySuppression {

  @SideEffectsOnly("nosuchfield")
  @SuppressWarnings("nullness")
  void modifies() {}

  @SuppressWarnings("nullness")
  void call() {
    modifies();
  }
}
