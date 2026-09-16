// At a call to a @SideEffectFree method, every argument passed to a functional-interface parameter
// must have a side-effect-free functional method:
//
//  * a lambda:  its body must be side-effect-free;
//  * a method reference:  the referenced method must be @SideEffectFree;
//  * a functional-interface parameter of an enclosing @SideEffectFree method:  the caller of that
//    method has already discharged the obligation;
//  * anything else:  checked against the functional method of the argument's declared type.
//
// Each call site is in an unannotated method, so the only diagnostic a call site can produce is the
// one under test.  The callees do not call their parameters; that is tested in
// PurityFunctionalParameter.java.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalArgument {

  int count = 0;

  /** A functional interface whose functional method is annotated. */
  @FunctionalInterface
  interface PureFunction<T, R> extends Function<T, R> {
    @Override
    @SideEffectFree
    R apply(T t);
  }

  @SideEffectFree
  static int callee(Function<String, Integer> f, String s) {
    return 0;
  }

  static int unannotatedCallee(Function<String, Integer> f, String s) {
    return 0;
  }

  @SideEffectFree
  int pureLength(String s) {
    return s.length();
  }

  int impureLength(String s) {
    count++;
    return s.length();
  }

  Function<String, Integer> unannotatedVariable = s -> s.length();

  PureFunction<String, Integer> annotatedVariable = s -> s.length();

  Function<String, Integer> lengthFunction() {
    return s -> s.length();
  }

  PureFunction<String, Integer> pureLengthFunction() {
    return s -> s.length();
  }

  // A lambda argument's body is checked directly.

  void lambdaArguments(String s) {
    callee(t -> t.length(), s);
    // :: error: [purity.not.sideeffectfree.assign.field]
    callee(t -> count++, s);
    // :: error: [purity.not.sideeffectfree.call]
    callee(t -> impureLength(t), s);
    callee(
        t -> {
          int local = 0;
          local++;
          return local;
        },
        s);
  }

  // The method that a method reference refers to must be @SideEffectFree.

  void methodReferenceArguments(String s) {
    callee(this::pureLength, s);
    // :: error: [purity.functional.argument]
    callee(this::impureLength, s);
    callee(String::length, s);
  }

  // Any other argument is checked against the functional method of its declared type.

  void otherArguments(String s) {
    // :: error: [purity.functional.argument]
    callee(unannotatedVariable, s);
    callee(annotatedVariable, s);
    // :: error: [purity.functional.argument]
    callee(lengthFunction(), s);
    callee(pureLengthFunction(), s);
  }

  // A functional-interface parameter may be passed onward from a @SideEffectFree method, but not
  // from a method that makes no promise about what it was given.

  @SideEffectFree
  int passesParameterOnward(Function<String, Integer> f, String s) {
    return callee(f, s);
  }

  int unannotatedMethodPassesParameterOnward(Function<String, Integer> f, String s) {
    // :: error: [purity.functional.argument]
    return callee(f, s);
  }

  // An unannotated callee imposes no obligation on its arguments.

  void unannotatedCalleeTakesAnything(String s) {
    unannotatedCallee(t -> count++, s);
    unannotatedCallee(this::impureLength, s);
    unannotatedCallee(unannotatedVariable, s);
  }

  // An argument that is not of functional-interface type is unaffected.

  @SideEffectFree
  static int ordinaryParameters(String s, int i) {
    return i;
  }

  void ordinaryArguments(String s) {
    ordinaryParameters(s, count);
  }

  // A method reference assigned to an annotated functional interface is checked against its
  // functional method, wherever the assignment appears.

  void methodReferenceAgainstAnnotatedSam() {
    PureFunction<String, Integer> ok = this::pureLength;
    // :: error: [purity.methodref]
    PureFunction<String, Integer> bad = this::impureLength;
  }
}
