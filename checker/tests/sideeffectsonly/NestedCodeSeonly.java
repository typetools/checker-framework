// Code that is written inside a method but runs elsewhere -- the body of a lambda, or the body of
// a method of a class that is declared within the method -- is not a side effect of the enclosing
// method.  Each place where such code runs is checked on its own.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class NestedCodeSeonly {

  static int staticField;

  interface Runner {
    void run();
  }

  @SideEffectsOnly("this")
  void storesALambda() {
    Runner r = () -> staticField = 1;
  }

  @SideEffectsOnly("this")
  void invokesALambda() {
    Runner r = () -> staticField = 1;
    // `Runner.run` has no side-effect annotation, so it might modify anything.
    // :: error: (purity.unknown.sideeffectsonly)
    r.run();
  }

  @SideEffectsOnly("this")
  void declaresALocalClass() {
    class Local {
      int ownField;

      void modify() {
        ownField = 1;
        staticField = 1;
      }
    }
  }

  // When the functional interface method is annotated, the lambda's body is checked against that
  // annotation, at the lambda.

  interface AnnotatedRunner {
    @SideEffectsOnly("this")
    void run();
  }

  interface Adder {
    @SideEffectsOnly("#1")
    void add(java.util.List<String> lst);
  }

  @SideEffectsOnly("this")
  void lambdaModifiesStaticField() {
    // `this` in `AnnotatedRunner.run`'s annotation is the object that evaluating the lambda
    // expression creates, so the body may modify nothing at all.
    // :: error: (purity.incorrect.sideeffectsonly)
    AnnotatedRunner r = () -> staticField = 1;
  }

  @SideEffectsOnly("this")
  void lambdaModifiesItsParameter() {
    // `#1` of `Adder.add` is the lambda's own parameter.
    Adder a = lst -> lst.add("x");
  }

  @SideEffectsOnly("this")
  void lambdaModifiesAnotherList(java.util.List<String> other) {
    // :: error: (purity.incorrect.sideeffectsonly)
    Adder a = lst -> other.add("x");
  }

  // A lambda that is passed to a call might run before the call returns.  When the functional
  // interface method has no side-effect annotation -- as `Predicate.test` does not -- the callee's
  // own annotation does not account for what the invocation modifies, so the lambda's body is
  // checked as part of this method.

  @SideEffectsOnly("#1")
  void passesALambdaThatModifiesAStaticField(java.util.List<String> lst) {
    lst.removeIf(
        s -> {
          // :: error: (purity.incorrect.sideeffectsonly)
          staticField = 1;
          return true;
        });
  }

  @SideEffectsOnly("#1")
  void passesAHarmlessLambda(java.util.List<String> lst) {
    lst.removeIf(s -> s.isEmpty());
  }

  @SideEffectsOnly("#1")
  void passesAPureMethodReference(java.util.List<String> lst) {
    lst.removeIf(String::isEmpty);
  }

  @SideEffectsOnly("#1")
  void passesAPredicateVariable(
      java.util.List<String> lst, java.util.function.Predicate<String> p) {
    // The body that `p` holds is not at hand to be checked, so what invoking it modifies is
    // unknown.
    // :: error: (purity.unknown.sideeffectsonly)
    lst.removeIf(p);
  }

  @SideEffectsOnly("this")
  void createsAnAnonymousClass() {
    // Creating the object runs the anonymous class's constructor, which has no side-effect
    // annotation.  The diagnostic names the anonymous class by its supertype.
    Runner r =
        // :: error: (purity.unknown.sideeffectsonly)
        new Runner() {
          @Override
          public void run() {
            staticField = 1;
          }
        };
  }
}
