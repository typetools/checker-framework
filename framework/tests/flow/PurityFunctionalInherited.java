// A class can implement an interface method by inheriting a method from its superclass. A call
// through the interface checks its arguments against the interface method, so the inherited method
// cannot assume that its functional-interface argument is side-effect-free unless the interface
// method also requires that.
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

  // Base.apply implements Unannotated.apply only in this class, so Base.apply's assumption
  // about its argument does not hold for calls through Unannotated.
  // :: error: [purity.functional.parameter.inherited]
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

  /** A subclass that overrides the method is checked against the interface method as usual. */
  static class SubOverrides extends Base implements Unannotated {
    @Override
    @SideEffectFree
    public int apply(Supplier<Integer> s) {
      // :: error: [purity.call]
      return s.get();
    }
  }
}
