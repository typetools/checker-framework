// The Lock Checker ignores `@SideEffectsOnly`, which constrains which expressions a method
// modifies but promises nothing about acquiring or releasing locks.  A `@SideEffectsOnly` method
// therefore gets the same locking guarantee as an unannotated one, `@ReleasesNoLocks`.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SideEffectsOnlyLock {

  Object field;

  void unannotated() {}

  @SideEffectsOnly("this")
  void callsUnannotatedMethod() {
    unannotated();
  }

  @SideEffectsOnly("this")
  synchronized void synchronizedMethod() {
    field = null;
  }
}
