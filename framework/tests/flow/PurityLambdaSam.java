// Test that a lambda's body is checked against the purity annotations on the functional interface
// method that the lambda implements.  Unlike an overriding method, a lambda does not inherit the
// annotation, and unlike a method reference, a lambda has no declaration to compare against, so
// nothing else performs this check.

import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityLambdaSam {

  int count = 0;

  @FunctionalInterface
  interface SefFunc {
    @SideEffectFree
    String get();
  }

  @FunctionalInterface
  interface DetFunc {
    @Deterministic
    String get();
  }

  @FunctionalInterface
  interface PureFunc {
    @Pure
    String get();
  }

  /** No purity annotation, so its lambdas are unconstrained. */
  @FunctionalInterface
  interface PlainFunc {
    String get();
  }

  String impure() {
    count++;
    return "";
  }

  @SideEffectFree
  String sefNotDeterministic() {
    return "";
  }

  @Pure
  String pure() {
    return "";
  }

  static class NotSideEffectFreeCtor {
    NotSideEffectFreeCtor() {}
  }

  // A @SideEffectFree functional method constrains the lambda's body.

  void sideEffectFree(int[] array, Runnable runnable) {
    SefFunc assignField =
        () -> {
          // :: error: [purity.assign.field]
          count++;
          return "";
        };
    // :: error: [purity.call]
    SefFunc call = () -> impure();
    SefFunc objectCreation =
        () -> {
          // :: error: [purity.call]
          new NotSideEffectFreeCtor();
          return "";
        };
    SefFunc assignArray =
        () -> {
          // :: error: [purity.assign.array]
          array[0] = 1;
          return "";
        };
    SefFunc callImpureFunctionalMethod =
        () -> {
          // :: error: [purity.call]
          runnable.run();
          return "";
        };
  }

  // The check does not depend on the lambda being the right-hand side of an assignment.

  String takesSefFunc(SefFunc f) {
    return f.get();
  }

  void otherLambdaPositions() {
    takesSefFunc(
        () -> {
          // :: error: [purity.assign.field]
          count++;
          return "";
        });
    Object cast =
        (SefFunc)
            () -> {
              // :: error: [purity.assign.field]
              count++;
              return "";
            };
  }

  SefFunc returnsLambda() {
    return () -> {
      // :: error: [purity.assign.field]
      count++;
      return "";
    };
  }

  /** Each lambda is checked against its own functional method, not against the outer one. */
  void nested() {
    SefFunc outer =
        () -> {
          SefFunc inner =
              () -> {
                // :: error: [purity.assign.field]
                count++;
                return "";
              };
          return inner.get();
        };
    PlainFunc unconstrainedOuter =
        () -> {
          SefFunc constrainedInner =
              () -> {
                // :: error: [purity.assign.field]
                count++;
                return "";
              };
          return constrainedInner.get();
        };
  }

  // @Deterministic and @Pure functional methods, which constrain different things.

  void deterministic() {
    // :: error: [purity.object.creation]
    DetFunc objectCreation = () -> new String("x");
    DetFunc catchBlock =
        () -> {
          try {
            return "";
            // :: error: [purity.catch]
          } catch (RuntimeException e) {
            return "e";
          }
        };
    // A @SideEffectFree but non-@Deterministic callee is fine for SefFunc and not for DetFunc.
    SefFunc okHere = () -> sefNotDeterministic();
    // :: error: [purity.call]
    DetFunc notOkHere = () -> sefNotDeterministic();
    // A field assignment breaks both kinds, so it is reported here too.
    DetFunc assignField =
        () -> {
          // :: error: [purity.assign.field]
          count++;
          return "";
        };
  }

  void pureFunctionalMethod() {
    PureFunc both =
        () -> {
          // :: error: [purity.assign.field]
          count++;
          // :: error: [purity.object.creation]
          return new String("x");
        };
    PureFunc ok = () -> pure();
  }

  // A constructor may assign to its own class's fields, but a lambda that a constructor creates
  // may run after construction has finished, so the lambda's body does not get that permission.

  static class InConstructor {
    int field;
    SefFunc f;

    InConstructor() {
      field = 1;
      f =
          () -> {
            // :: error: [purity.assign.field]
            field = 2;
            return "";
          };
    }
  }

  // Lambdas that are accepted.

  void accepted(String s) {
    SefFunc constant = () -> "";
    SefFunc callsSideEffectFree = () -> sefNotDeterministic();
    SefFunc localVariable =
        () -> {
          int local = 0;
          local++;
          return "" + local;
        };
    DetFunc callsPure = () -> pure();
    // An unannotated functional method promises nothing, so its lambda is unconstrained.
    PlainFunc unconstrained =
        () -> {
          count++;
          return impure();
        };
    Runnable jdkRunnable = () -> count++;
  }
}
