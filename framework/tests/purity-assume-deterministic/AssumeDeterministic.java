// -AassumeDeterministic assumes that every called method is deterministic, including a method with
// no purity annotation.  It assumes nothing about side effects.

import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class AssumeDeterministic {

  int field = 0;

  int unannotated() {
    field++;
    return field;
  }

  @Deterministic
  int deterministicCall() {
    return unannotated();
  }

  @SideEffectFree
  int sideEffectFreeCall() {
    // :: error: [purity.not.sideeffectfree.call]
    return unannotated();
  }

  @Pure
  int pureCall() {
    // :: error: [purity.not.sideeffectfree.call]
    return unannotated();
  }

  /** The assumption is about the methods that a body calls, not about the body itself. */
  @Deterministic
  int bodyIsStillChecked() {
    // :: error: [purity.not.deterministic.assign.field]
    field++;
    return field;
  }
}
