// Test that PurityChecker does not attribute the effects of a lambda body, or of a local or
// anonymous class body, to the enclosing method.  Evaluating a lambda expression or declaring a
// class does not run the code in it; those effects occur where the functional method or the
// class's method is invoked, and each such invocation is checked like any other method call.

import java.util.function.Supplier;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class PurityLambda {

  int count = 0;

  void impureVoid() {
    count++;
  }

  String impureString() {
    count++;
    return "";
  }

  void takesRunnable(Runnable r) {}

  // Creating an impure lambda is not an effect of the enclosing method.

  @SideEffectFree
  Runnable returnsImpureLambda() {
    return () -> count++;
  }

  @SideEffectFree
  String storesImpureLambda() {
    Supplier<String> s = () -> impureString();
    return "";
  }

  @SideEffectFree
  Supplier<Runnable> returnsNestedImpureLambda() {
    return () -> (Runnable) () -> count++;
  }

  // Creating a lambda is creating an object, which is not deterministic, just as a `new`
  // expression is not.

  @Deterministic
  Supplier<String> returnsNondeterministicLambda() {
    // :: error: [purity.not.deterministic.object.creation]
    return () -> new String("x");
  }

  @Deterministic
  Supplier<String> returnsNondeterministicLambdaWithPureBody() {
    // :: error: [purity.not.deterministic.object.creation]
    return () -> "x";
  }

  @SideEffectFree
  Supplier<String> createsLambdaInSideEffectFreeMethod() {
    // No error:  creating an object has no side effect.
    return () -> "x";
  }

  @SideEffectsOnly("this.count")
  Runnable sideEffectsOnlyReturnsImpureLambda() {
    return () -> impureVoid();
  }

  // Declaring a local or anonymous class is not an effect of the enclosing method, but
  // instantiating one is: neither an anonymous class's implicit constructor nor this local
  // class's default constructor is @SideEffectFree.

  @SideEffectFree
  Runnable anonymousClass() {
    // :: error: [purity.not.sideeffectfree.call]
    return new Runnable() {
      @Override
      public void run() {
        count++;
      }
    };
  }

  @SideEffectFree
  Object localClass() {
    class Local {
      void go() {
        count++;
      }
    }
    // :: error: [purity.not.sideeffectfree.call]
    return new Local();
  }

  // A field initializer or initializer block is part of no method declaration, so it is checked
  // against no purity annotation of its own.  It runs when the class is instantiated, and the
  // Purity Checker conservatively attributes it to the method that declares the class.

  @SideEffectFree
  Object localClassInitializer() {
    class Local {
      @SideEffectFree
      Local() {}

      // :: error: [purity.not.sideeffectfree.assign.field]
      int x = count++;

      {
        // :: error: [purity.not.sideeffectfree.assign.field]
        count++;
      }
    }
    // No error:  the constructor is @SideEffectFree.
    return new Local();
  }

  @SideEffectFree
  Object localClassInitializesOwnField() {
    class Local {
      int y;

      // No error:  assigning a field of the class being initialized is like a constructor.
      int z = 1;

      {
        y = 1;
      }
    }
    // :: error: [purity.not.sideeffectfree.call]
    return new Local();
  }

  // Effects that are still the enclosing method's.

  @SideEffectFree
  String createsAndInvokes() {
    Runnable r = () -> count++;
    // :: error: [purity.not.sideeffectfree.call]
    r.run();
    return "";
  }

  @SideEffectFree
  String passesImpureLambda() {
    // :: error: [purity.not.sideeffectfree.call]
    takesRunnable(() -> count++);
    return "";
  }

  @SideEffectFree
  Runnable effectOutsideLambda() {
    // :: error: [purity.not.sideeffectfree.assign.field]
    count++;
    return () -> {};
  }

  @SideEffectFree
  Supplier<String> effectInCapturedExpression() {
    // :: error: [purity.not.sideeffectfree.call]
    String captured = impureString();
    return () -> captured;
  }
}
