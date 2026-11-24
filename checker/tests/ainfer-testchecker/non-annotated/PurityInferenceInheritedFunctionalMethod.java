// Inference of purity for the methods through which a call can run a lambda or a method reference
// whose functional interface inherits its functional method from two superinterfaces.  A call
// through either superinterface's method can run the lambda or method reference, so inference must
// account for it in both methods.  A pure lambda of each superinterface's type makes inference
// infer @Pure for that superinterface's method.  If inference ignored the impure lambda or method
// reference for either method, then the validation run would report purity.methodref or
// purity.call.

public class PurityInferenceInheritedFunctionalMethod {

  int count;

  Object impure() {
    count++;
    return null;
  }

  interface RefA {
    Object run();
  }

  interface RefB {
    Object run();
  }

  interface RefAB extends RefA, RefB {}

  interface RefBA extends RefB, RefA {}

  void methodReferences() {
    RefA a = () -> null;
    RefB b = () -> null;
    RefAB ab = this::impure;
    RefBA ba = this::impure;
  }

  interface LambdaA {
    Object run();
  }

  interface LambdaB {
    Object run();
  }

  interface LambdaAB extends LambdaA, LambdaB {}

  interface LambdaBA extends LambdaB, LambdaA {}

  void lambdas() {
    LambdaA a = () -> null;
    LambdaB b = () -> null;
    LambdaAB ab = () -> impure();
    LambdaBA ba = () -> impure();
  }
}
