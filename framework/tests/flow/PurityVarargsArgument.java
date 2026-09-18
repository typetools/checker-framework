// At a call to a @SideEffectFree method, an argument passed to a varargs functional-interface
// parameter is checked like an argument passed to any other functional-interface parameter.
//
// In the expanded form of such a call, each argument from the varargs parameter onward is an
// element of its array, and is checked against the array's component type.  In the array form,
// the argument is the array itself:  each initializer of an array literal is checked, and the
// elements of any other array are unknown and therefore unchecked.

import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityVarargsArgument {

  int count = 0;

  @SideEffectFree
  static int callee(Runnable... rs) {
    return 0;
  }

  @SideEffectFree
  static int calleeWithLeadingParameter(String s, Runnable... rs) {
    return 0;
  }

  @SideEffectFree
  static int calleeWithoutFunctionalParameter(String... ss) {
    return 0;
  }

  void impureVoid() {
    count++;
  }

  @SideEffectFree
  void pureVoid() {}

  // The expanded form:  each argument is one element of the array.

  void expandedForm(String s) {
    callee(() -> {});
    callee(this::pureVoid);
    // :: error: [purity.not.sideeffectfree.assign.field]
    callee(() -> count++);
    // :: error: [purity.functional.argument]
    callee(this::impureVoid);
    callee(this::pureVoid, this::pureVoid);
    // :: error: [purity.functional.argument]
    callee(this::pureVoid, this::impureVoid);
    // :: error: [purity.functional.argument]
    calleeWithLeadingParameter(s, this::impureVoid);
  }

  void zeroVarargsArguments() {
    callee();
  }

  // The array form:  the argument is the array that the call passes.

  void arrayForm(Runnable[] rs) {
    callee(new Runnable[] {this::pureVoid});
    // :: error: [purity.functional.argument]
    callee(new Runnable[] {this::impureVoid});
    // :: error: [purity.not.sideeffectfree.assign.field]
    callee(new Runnable[] {() -> count++});
    // An array that the call receives from elsewhere says nothing about its elements.
    callee(rs);
    // Neither does an array with no initializer.
    callee(new Runnable[10]);
  }

  // A varargs parameter whose component type is not a functional interface is unaffected.

  void notFunctional() {
    calleeWithoutFunctionalParameter("a", "b");
    calleeWithoutFunctionalParameter(new String[] {"a"});
  }
}
