// At a call to a @SideEffectFree method, an argument passed to a varargs parameter is not checked,
// even if the array's component type is a functional interface.  The body of the method may not
// assume anything about the elements of the array, because the assumption applies only to a call
// of the functional method on a parameter itself.

import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityVarargsArgument {

  int count = 0;

  @SideEffectFree
  static int callee(Runnable... rs) {
    return 0;
  }

  @SideEffectFree
  static void runsElements(Runnable... rs) {
    // :: error: [purity.call]
    rs[0].run();
    for (Runnable r : rs) {
      // :: error: [purity.call]
      r.run();
    }
  }

  @SideEffectFree
  static int calleeWithLeadingParameter(Runnable r, Runnable... rs) {
    return 0;
  }

  void impureVoid() {
    count++;
  }

  void expandedForm() {
    callee(() -> count++);
    callee(this::impureVoid, this::impureVoid);
    // The parameter before the varargs parameter is checked as usual.
    // :: error: [purity.functional.argument]
    calleeWithLeadingParameter(this::impureVoid, this::impureVoid);
  }

  void arrayForm(Runnable[] rs) {
    callee(new Runnable[] {this::impureVoid});
    callee(new Runnable[] {() -> count++});
    callee(rs);
  }
}
