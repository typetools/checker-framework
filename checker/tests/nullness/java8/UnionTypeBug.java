// Test case for Issue 384:
// https://github.com/typetools/checker-framework/issues/384

// TODO: no longer crashes, but still need to ensure annotations
// are stored correctly.

import org.checkerframework.checker.guieffect.qual.PolyUIType;
import org.checkerframework.checker.nullness.qual.NonNull;

abstract class UnionTypeBug {

  void method() {
    try {

      badBoy();

      // :: warning: (nullness.on.exception.parameter)
    } catch (@NonNull InnerException1 | @NonNull InnerException2 e) {

      // :: warning: (nullness.on.exception.parameter)
    } catch (@NonNull InnerException3 | @NonNull InnerException4 e) {

    }
  }

  abstract void badBoy() throws InnerException1, InnerException2, InnerException3, InnerException4;

  @PolyUIType
  class InnerException1 extends Exception {}

  @PolyUIType
  class InnerException2 extends Exception {}

  @PolyUIType
  class InnerException3 extends Exception {}

  @PolyUIType
  class InnerException4 extends Exception {}
}
