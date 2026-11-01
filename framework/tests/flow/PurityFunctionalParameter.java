// A @SideEffectFree method may call the functional method of one of its own functional-interface
// parameters; that call is side-effect-free.
//
// The receiver must be the parameter itself.  A local that aliases the parameter, a field, or the
// result of a call does not get the assumption, and neither does a method of the parameter's type
// other than the functional method.

import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalParameter {

  int count = 0;

  @FunctionalInterface
  interface PureFunc {
    @SideEffectFree
    String get();
  }

  Function<String, Integer> field = s -> s.length();

  PureFunc pureField = () -> "";

  @SideEffectFree
  Function<String, Integer> lengthFunction() {
    return s -> s.length();
  }

  // The receiver is one of this method's own parameters.

  @SideEffectFree
  int callsParameter(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @SideEffectFree
  int callsTwoParameters(Function<String, Integer> f, Function<String, Integer> g, String s) {
    int a = f.apply(s);
    int b = g.apply(s);
    return a + b;
  }

  @SideEffectFree
  String callsSupplierParameter(Supplier<String> supplier) {
    return supplier.get();
  }

  /** A parameter whose type is a type variable bounded by a functional interface. */
  @SideEffectFree
  <T extends Function<String, Integer>> int callsTypeVariableParameter(T f, String s) {
    return f.apply(s);
  }

  // Only the parameter itself is a legal receiver.

  @SideEffectFree
  int callsField(String s) {
    // :: error: [purity.call]
    return field.apply(s);
  }

  @SideEffectFree
  int callsLocalAlias(Function<String, Integer> f, String s) {
    Function<String, Integer> g = f;
    // :: error: [purity.call]
    return g.apply(s);
  }

  @SideEffectFree
  int callsCallResult(String s) {
    // :: error: [purity.call]
    return lengthFunction().apply(s);
  }

  // A functional interface whose functional method is annotated is side-effect-free wherever it is
  // called, so these need no assumption about the receiver.

  @SideEffectFree
  String annotatedSamField() {
    return pureField.get();
  }

  @SideEffectFree
  String annotatedSamLocalAlias(PureFunc p) {
    PureFunc q = p;
    return q.get();
  }

  @SideEffectFree
  <T extends PureFunc> String annotatedSamTypeVariable(T t) {
    return t.get();
  }

  // Only the functional method is assumed side-effect-free.

  @SideEffectFree
  Function<String, Integer> callsNonFunctionalMethod(
      Function<String, Integer> f, Function<Integer, Integer> g) {
    // :: error: [purity.call]
    return f.andThen(g);
  }

  /** The rest of the body is checked as usual. */
  @SideEffectFree
  int otherEffectsAreReported(Function<String, Integer> f, String s) {
    // :: error: [purity.assign.field]
    count++;
    return f.apply(s);
  }

  // A lambda's body may call a functional-interface parameter of the enclosing method, which
  // holds a value that the caller of that method was required to check, whenever the lambda runs.

  @SideEffectFree
  PureFunc lambdaCallsParameter(Function<String, Integer> f) {
    return () -> "" + f.apply("x");
  }

  PureFunc unannotatedMethodsLambdaCallsParameter(Function<String, Integer> f) {
    // :: error: [purity.call]
    return () -> "" + f.apply("x");
  }

  /** An unannotated method promises nothing, so nothing in its body is checked. */
  int unannotatedEnclosingMethod(Function<String, Integer> f, String s) {
    count++;
    return f.apply(s);
  }
}
