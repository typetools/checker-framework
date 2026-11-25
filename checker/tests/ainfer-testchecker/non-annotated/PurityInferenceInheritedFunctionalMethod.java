// Inference of purity for the methods through which a call can run a lambda or a method reference
// whose functional interface inherits its functional method from two superinterfaces.  A call
// through either superinterface's method can run the lambda or method reference, so inference must
// account for it in both methods.  A pure lambda of each superinterface's type makes inference
// infer @Pure for that superinterface's method.  If inference ignored the impure lambda or method
// reference for either method, then the validation run would report purity.methodref or
// purity.call.
//
// Each group of interfaces is used once, with its superinterfaces in one order, so that whichever
// superinterface's method javac chooses as the functional method, some group has a superinterface
// method that is not the functional method.

public class PurityInferenceInheritedFunctionalMethod {

  int count;

  Object impure() {
    count++;
    return null;
  }

  interface RefA1 {
    Object run();
  }

  interface RefB1 {
    Object run();
  }

  interface RefA1B1 extends RefA1, RefB1 {}

  interface RefA2 {
    Object run();
  }

  interface RefB2 {
    Object run();
  }

  interface RefB2A2 extends RefB2, RefA2 {}

  void methodReferences() {
    RefA1 a1 = () -> null;
    RefB1 b1 = () -> null;
    RefA1B1 a1b1 = this::impure;
    RefA2 a2 = () -> null;
    RefB2 b2 = () -> null;
    RefB2A2 b2a2 = this::impure;
  }

  interface LambdaA1 {
    Object run();
  }

  interface LambdaB1 {
    Object run();
  }

  interface LambdaA1B1 extends LambdaA1, LambdaB1 {}

  interface LambdaA2 {
    Object run();
  }

  interface LambdaB2 {
    Object run();
  }

  interface LambdaB2A2 extends LambdaB2, LambdaA2 {}

  void lambdas() {
    LambdaA1 a1 = () -> null;
    LambdaB1 b1 = () -> null;
    LambdaA1B1 a1b1 = () -> impure();
    LambdaA2 a2 = () -> null;
    LambdaB2 b2 = () -> null;
    LambdaB2A2 b2a2 = () -> impure();
  }
}
