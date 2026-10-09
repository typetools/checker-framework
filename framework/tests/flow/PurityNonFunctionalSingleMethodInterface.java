// A parameter holds code, and its functional method may be assumed pure in a method with a purity
// annotation, only if its type or the interface that declares its functional method is annotated
// with @FunctionalInterface.  An interface such as Iterable or AutoCloseable has a single abstract
// method, but a parameter of that type holds an ordinary object.
//
// Each call site is in an unannotated method, so the only diagnostic a call site can produce is the
// one under test.

import java.io.FileInputStream;
import java.util.List;
import java.util.function.Function;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityNonFunctionalSingleMethodInterface {

  int count = 0;

  @Pure
  static boolean isEmpty(Iterable<?> it) {
    // :: error: [purity.call]
    return !it.iterator().hasNext();
  }

  @SideEffectFree
  static String closeQuietly(AutoCloseable r) {
    try {
      // :: error: [purity.call]
      r.close();
    } catch (Exception e) {
    }
    return "";
  }

  /** A single-method interface without @FunctionalInterface. */
  interface Action {
    void act();
  }

  @SideEffectFree
  static void runAction(Action a) {
    // :: error: [purity.call]
    a.act();
  }

  /** The functional method is declared in Function, which is annotated. */
  interface StringFunction extends Function<String, Integer> {}

  @SideEffectFree
  static int applyStringFunction(StringFunction f) {
    return f.apply("");
  }

  void callers(List<String> list, FileInputStream in) {
    boolean b = isEmpty(list);
    String d = closeQuietly(in);
    runAction(() -> count++);
    // :: error: [purity.assign.field]
    applyStringFunction(s -> count++);
  }
}
