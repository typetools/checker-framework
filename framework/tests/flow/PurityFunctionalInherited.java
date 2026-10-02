// A class can implement an interface method by inheriting a method from its superclass. A call
// through the interface checks its arguments against the interface method, but such a call does not
// rely on the inherited method's purity either, so the class is not an error.
//
// Each call site is in an unannotated method, so the only diagnostic a call site can produce is the
// one under test.

import java.util.function.Supplier;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalInherited {

  int count = 0;

  static class Base {
    /** The body assumes that {@code s} is side-effect-free. */
    @SideEffectFree
    public int apply(Supplier<Integer> s) {
      return s.get();
    }
  }

  /** The interface method imposes nothing on its argument. */
  interface Unannotated {
    int apply(Supplier<Integer> s);
  }

  // Base.apply implements Unannotated.apply only in this class. A call through Unannotated may pass
  // an argument with side effects, but the caller does not treat the call as side-effect-free.
  static class Sub extends Base implements Unannotated {}

  void callsThroughInterface(Unannotated receiver) {
    // This call reaches Base.apply when receiver is a Sub.
    receiver.apply(() -> count++);
  }

  void callsThroughBase(Base receiver) {
    receiver.apply(() -> 0);
    // :: error: [purity.assign.field]
    receiver.apply(() -> count++);
  }

  /** The interface method requires what Base.apply assumes. */
  interface Annotated {
    @SideEffectFree
    int apply(Supplier<Integer> s);
  }

  static class SubOfAnnotated extends Base implements Annotated {}

  void callsThroughAnnotatedInterface(Annotated receiver) {
    receiver.apply(() -> 0);
    // :: error: [purity.assign.field]
    receiver.apply(() -> count++);
  }

  /**
   * An override, like an inherited method, may assume what its own annotation requires of its
   * argument: a call through Unannotated does not rely on the override's purity.
   */
  static class SubOverrides extends Base implements Unannotated {
    @Override
    @SideEffectFree
    public int apply(Supplier<Integer> s) {
      return s.get();
    }
  }

  /** The body relies on the purity of calls to u.apply, but those calls check no argument. */
  @SideEffectFree
  static int runsUnannotated(Unannotated u) {
    return u.apply(() -> 0);
  }

  /** The body relies on the purity of calls to a.apply, and those calls check their argument. */
  @SideEffectFree
  static int runsAnnotated(Annotated a) {
    return a.apply(() -> 0);
  }

  void passesImplementations() {
    // :: error: [purity.functional.argument.parameter]
    runsUnannotated(new Sub());
    // :: error: [purity.functional.argument.parameter]
    runsUnannotated(new SubOverrides());
    runsAnnotated(new SubOfAnnotated());
  }
}
