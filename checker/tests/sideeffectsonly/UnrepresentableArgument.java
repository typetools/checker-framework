// A callee's `@SideEffectsOnly` annotation cannot always be written at a call site:  view
// adaptation may yield an expression that the checker cannot represent, as it does when an argument
// is a conditional expression.  Then the checker does not know what the call modifies, so it
// discards every refinement.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class UnrepresentableArgument {

  @Tainted Object a;
  @Tainted Object b;

  @EnsuresQualifier(expression = "#1", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void makeUntainted(Object o) {}

  @SideEffectsOnly("#1")
  void modifies(Object o) {}

  void representableArgument() {
    makeUntainted(a);
    modifies(a);
    // :: error: assignment
    @Untainted Object y = a;
  }

  void unrepresentableArgument(boolean cond) {
    makeUntainted(a);
    modifies(cond ? a : b);
    // :: error: assignment
    @Untainted Object y = a;
  }

  void otherArgumentIsModified() {
    makeUntainted(a);
    // The call modifies only `this.b`, so the refinement of `this.a` survives.
    modifies(b);
    @Untainted Object y = a;
  }
}
