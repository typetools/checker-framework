// The obligation on the arguments of a @SideEffectFree method with a functional-interface
// parameter is inherited by an override, and a call site is checked against the declaration of its
// static target.
//
// Each call site is in an unannotated method, so the only diagnostic a call site can produce is the
// one under test.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalOverride {

  int count = 0;

  static class Super {
    @SideEffectFree
    int m(Function<String, Integer> f, String s) {
      return 0;
    }
  }

  static class Sub extends Super {
    /**
     * @SideEffectFree is inherited, so this body is checked as side-effect-free.
     */
    @Override
    int m(Function<String, Integer> f, String s) {
      return f.apply(s);
    }
  }

  static class SubRepeatsAnnotation extends Super {
    @Override
    @SideEffectFree
    int m(Function<String, Integer> f, String s) {
      return 0;
    }
  }

  void callsThroughSupertype(Super receiver, String s) {
    receiver.m(t -> t.length(), s);
    // :: error: [purity.not.sideeffectfree.assign.field]
    receiver.m(t -> count++, s);
  }

  void callsThroughSubtype(Sub receiver, String s) {
    receiver.m(t -> t.length(), s);
    // :: error: [purity.not.sideeffectfree.assign.field]
    receiver.m(t -> count++, s);
  }

  /** An unrelated unannotated method imposes nothing on its arguments. */
  static class Unrelated {
    int m(Function<String, Integer> f, String s) {
      return 0;
    }
  }

  void callsUnannotated(Unrelated receiver, String s) {
    receiver.m(t -> count++, s);
  }

  static class GenericSuper<T> {
    @SideEffectFree
    int m(T f, String s) {
      return 0;
    }
  }

  /**
   * A call through GenericSuper checks nothing about the argument, whose declared type is a type
   * variable, so this body cannot assume that the argument is side-effect-free.
   */
  static class GenericSub extends GenericSuper<Function<String, Integer>> {
    @Override
    int m(Function<String, Integer> f, String s) {
      // :: error: [purity.not.sideeffectfree.call]
      return f.apply(s);
    }
  }

  void callsThroughGenericSupertype(GenericSuper<Function<String, Integer>> receiver, String s) {
    receiver.m(t -> count++, s);
  }

  /** A call through Unrelated checks nothing, so the override cannot assume anything. */
  static class AnnotatedSubOfUnrelated extends Unrelated {
    @Override
    @SideEffectFree
    int m(Function<String, Integer> f, String s) {
      // :: error: [purity.not.sideeffectfree.call]
      return f.apply(s);
    }
  }
}
