// -AassumeSideEffectFree assumes that every called method is side-effect-free, including a method
// with no purity annotation.  It assumes nothing about determinism.

import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class AssumeSideEffectFree {

  int field = 0;

  int unannotated() {
    field++;
    return field;
  }

  void unannotatedVoid() {
    field++;
  }

  @SideEffectFree
  int sideEffectFreeCall() {
    return unannotated();
  }

  @Deterministic
  int deterministicCall() {
    // :: error: [purity.call]
    return unannotated();
  }

  @Pure
  int pureCall() {
    // :: error: [purity.call]
    return unannotated();
  }

  /**
   * A method that is assumed to have no side effect and that returns no value changes nothing and
   * yields nothing, so calling it leaves the caller deterministic.
   */
  @Pure
  int pureVoidCall() {
    unannotatedVoid();
    return 0;
  }

  /** The assumption is about the methods that a body calls, not about the body itself. */
  @SideEffectFree
  int bodyIsStillChecked() {
    // :: error: [purity.assign.field]
    field++;
    return field;
  }
}
