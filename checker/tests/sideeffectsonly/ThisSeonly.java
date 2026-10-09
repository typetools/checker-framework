// Assigning to a field of `this` is covered by `@SideEffectsOnly("this")`, whether or not the
// receiver is written explicitly.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ThisSeonly {

  Object f;
  int counter;
  ThisSeonly other;
  static Object staticField;

  @SideEffectsOnly("this")
  void implicitReceiver() {
    f = null;
  }

  @SideEffectsOnly("this")
  void explicitReceiver() {
    this.f = null;
  }

  @SideEffectsOnly("this")
  void compoundAssignmentAndIncrement() {
    counter += 1;
    this.counter++;
    ++counter;
  }

  @SideEffectsOnly("this.f")
  void justOneField() {
    this.f = null;
    // :: error: (purity.incorrect.sideeffectsonly)
    this.counter = 0;
  }

  @SideEffectsOnly("this")
  void transitivelyReachableField() {
    // `this` is the receiver of `this.other`, which is the receiver of `this.other.f`, so this
    // assignment is covered, just as `@SideEffectsOnly("#1")` covers assignments to `#1.f.g`.
    this.other.f = null;
  }

  @SideEffectsOnly("this")
  void staticFieldIsNotThis() {
    // :: error: (purity.incorrect.sideeffectsonly)
    staticField = null;
  }
}
