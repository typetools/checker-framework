// A `@SideEffectsOnly` expression `#n` permits effects on the caller's argument.  If the method
// reassigns parameter `n`, then a write through the parameter might modify another object, so the
// parameter must be effectively final.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class ReassignedParameterSeonly {

  static class Node {
    int value;
  }

  static final Node SHARED = new Node();

  @SideEffectsOnly("#1")
  // :: error: (flowexpr.parameter.not.final)
  static void edit(Node p) {
    p = SHARED;
    p.value++;
  }

  @SideEffectsOnly("#1")
  static void editArgument(Node p) {
    p.value++;
  }

  @SideEffectsOnly("#1")
  // The second parameter is reassigned, but the annotation does not name it.
  static void reassignOther(Node p, Node q) {
    q = p;
    p.value++;
  }

  interface Editor {
    @SideEffectsOnly("#1")
    void apply(Node n);
  }

  void references() {
    Editor e1 = ReassignedParameterSeonly::editArgument;
    // The error about `edit` is reported at its declaration.
    Editor e2 = ReassignedParameterSeonly::edit;
  }
}
