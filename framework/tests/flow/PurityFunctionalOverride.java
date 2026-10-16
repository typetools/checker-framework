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
    // :: error: [purity.assign.field]
    receiver.m(t -> count++, s);
  }

  void callsThroughSubtype(Sub receiver, String s) {
    receiver.m(t -> t.length(), s);
    // :: error: [purity.assign.field]
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
   * A call through GenericSuper whose type argument is a type variable checks nothing about the
   * argument, so this body cannot assume that the argument is side-effect-free.
   */
  static class GenericSub extends GenericSuper<Function<String, Integer>> {
    @Override
    int m(Function<String, Integer> f, String s) {
      // :: error: [purity.call]
      return f.apply(s);
    }
  }

  /**
   * The declared type of the parameter is T, which is not a functional interface, so no override
   * may assume anything about the argument and the call checks nothing.
   */
  void callsThroughGenericSupertype(GenericSuper<Function<String, Integer>> receiver, String s) {
    receiver.m(t -> t.length(), s);
    receiver.m(t -> count++, s);
  }

  /** Here the type of the parameter at the call site is T, which is not a functional interface. */
  <T> void callsThroughTypeVariable(GenericSuper<T> receiver, T f, String s) {
    receiver.m(f, s);
  }

  /**
   * A call through Unrelated checks nothing, so the override may not assume that its argument is
   * side-effect-free: the body's own dataflow analysis would rely on that.
   */
  static class AnnotatedSubOfUnrelated extends Unrelated {
    @Override
    @SideEffectFree
    int m(Function<String, Integer> f, String s) {
      // :: error: [purity.call]
      return f.apply(s);
    }
  }
}
