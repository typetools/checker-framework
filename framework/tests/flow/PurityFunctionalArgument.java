// At a call to a @SideEffectFree method, every argument passed to a functional-interface parameter
// must have a side-effect-free functional method:
//
//  * a lambda:  its body must be side-effect-free;
//  * a method reference:  the referenced method must be @SideEffectFree;
//  * a conditional or switch expression:  each of its result expressions is checked on its own;
//  * a functional-interface parameter of an enclosing @SideEffectFree method:  the caller of that
//    method has already discharged the obligation;
//  * anything else:  checked against the functional method of the argument's declared type.
//
// Each call site is in an unannotated method, so the only diagnostic a call site can produce is the
// one under test.  The callees do not call their parameters; that is tested in
// PurityFunctionalParameter.java.

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
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

  // Each result expression of a conditional or switch expression is checked on its own.  The type
  // of such an expression is the parameter's type, which says nothing about the code that any
  // result expression denotes.

  void conditionalArguments(boolean b, String s) {
    callee(b ? t -> t.length() : t -> 0, s);
    // :: error: [purity.not.sideeffectfree.assign.field]
    callee(b ? t -> count++ : t -> 0, s);
    callee(b ? this::pureLength : String::length, s);
    // :: error: [purity.functional.argument]
    callee(b ? this::pureLength : this::impureLength, s);
  }

  void switchArguments(int i, String s) {
    callee(
        switch (i) {
          case 1 -> t -> t.length();
          default -> t -> 0;
        },
        s);
    callee(
        switch (i) {
          // :: error: [purity.not.sideeffectfree.assign.field]
          case 1 -> t -> count++;
          default -> t -> 0;
        },
        s);
    callee(
        switch (i) {
          case 1 -> this::pureLength;
          // :: error: [purity.functional.argument]
          default -> this::impureLength;
        },
        s);
  }

  // A result expression may also be the value of a `yield` statement.

  void yieldArguments(int i, String s) {
    callee(
        switch (i) {
          case 1:
            {
              // :: error: [purity.functional.argument]
              yield this::impureLength;
            }
          default:
            yield this::pureLength;
        },
        s);
  }

  // Parentheses do not change how an argument is checked.

  void parenthesizedArguments(boolean b, String s) {
    callee((t -> t.length()), s);
    // :: error: [purity.not.sideeffectfree.assign.field]
    callee((t -> count++), s);
    // :: error: [purity.functional.argument]
    callee((this::impureLength), s);
    // :: error: [purity.functional.argument]
    callee(b ? (this::pureLength) : (this::impureLength), s);
  }

  // The null literal denotes no code, so there is nothing to check.

  void nullArguments(boolean b, int i, String s) {
    callee(null, s);
    callee(b ? null : this::pureLength, s);
    callee(
        switch (i) {
          case 1 -> null;
          default -> this::pureLength;
        },
        s);
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

  // An argument whose type is a class that implements the functional interface is checked against
  // the class's implementation of the functional method, which is the code that will run.

  static class ImpureFunction implements Function<String, Integer> {
    int n = 0;

    @Override
    public Integer apply(String s) {
      return n++;
    }
  }

  static class SideEffectFreeFunction implements Function<String, Integer> {
    @Override
    @SideEffectFree
    public Integer apply(String s) {
      return s.length();
    }
  }

  void classTypedArguments(ImpureFunction impure, SideEffectFreeFunction pure, String s) {
    // :: error: [purity.functional.argument]
    callee(impure, s);
    callee(pure, s);
  }

  void anonymousClassArguments(String s) {
    callee(
        // :: error: [purity.functional.argument]
        new Function<String, Integer>() {
          @Override
          public Integer apply(String t) {
            return count++;
          }
        },
        s);
    callee(
        new Function<String, Integer>() {
          @Override
          @SideEffectFree
          public Integer apply(String t) {
            return t.length();
          }
        },
        s);
  }

  // A lambda's body may call a functional-interface parameter of the enclosing method, which holds
  // a value that the caller of that method was required to check, whenever the lambda runs.

  @SideEffectFree
  int lambdaCallsEnclosingParameter(Function<String, Integer> f, String s) {
    return callee(t -> f.apply(t), s);
  }

  int unannotatedMethodsLambdaCallsParameter(Function<String, Integer> f, String s) {
    // :: error: [purity.not.sideeffectfree.call]
    return callee(t -> f.apply(t), s);
  }

  /** A lambda's own parameter is checked at no call site, so passing it on is not permitted. */
  @SideEffectFree
  void lambdaParameterIsNotDischarged(Function<String, Integer> f, String s) {
    Consumer<Function<String, Integer>> c =
        g -> {
          // :: error: [purity.functional.argument]
          callee(g, s);
        };
    // :: error: [purity.not.sideeffectfree.call]
    c.accept(f);
  }

  // An array constructor cannot be annotated, and creating an array modifies nothing.

  @SideEffectFree
  static String[] arrayCallee(IntFunction<String[]> generator) {
    return generator.apply(0);
  }

  String[] arrayConstructorReference() {
    return arrayCallee(String[]::new);
  }

  // A method reference assigned to an annotated functional interface is checked against its
  // functional method, wherever the assignment appears.

  void methodReferenceAgainstAnnotatedSam() {
    PureFunction<String, Integer> ok = this::pureLength;
    // :: error: [purity.methodref]
    PureFunction<String, Integer> bad = this::impureLength;
  }
}
