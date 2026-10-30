// A class can implement an interface method by inheriting a method from its superclass. A call
// through the interface checks its arguments against the interface method, so the class is an error
// if the interface method does not guarantee what the inherited method assumes of its arguments.
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
  @FunctionalInterface
  interface Unannotated {
    int apply(Supplier<Integer> s);
  }

  // Base.apply implements Unannotated.apply only in this class. A call through Unannotated may pass
  // an argument with side effects.
  // :: error: [purity.inherited.functional.parameter]
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
  @FunctionalInterface
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
   * An override may assume only what every declaration it overrides guarantees of its argument, and
   * a call through Unannotated guarantees nothing.
   */
  static class SubOverrides extends Base implements Unannotated {
    @Override
    @SideEffectFree
    public int apply(Supplier<Integer> s) {
      // :: error: [purity.call]
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

  /** The errors are at the declarations of Sub and SubOverrides, not where they are used. */
  void passesImplementations() {
    runsUnannotated(new Sub());
    runsUnannotated(new SubOverrides());
    runsAnnotated(new SubOfAnnotated());
  }
}
