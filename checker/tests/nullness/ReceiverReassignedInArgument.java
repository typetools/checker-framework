import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

// A call's receiver is evaluated before its arguments.  If an argument reassigns the receiver
// expression, then normal completion of the call does not show that the receiver expression is
// non-null afterward.  Likewise for an argument that a later argument reassigns.
// ReceiverRefinementInCatch.java contains more such tests.
public class ReceiverReassignedInArgument {

  @Nullable ReceiverReassignedInArgument f;

  void take(@Nullable Object o) {}

  @SideEffectFree
  void sefTake(@Nullable Object o) {}

  void take(Object first, @Nullable Object second) {}

  // An argument passed to a non-null parameter is refined to non-null after the call, but not if a
  // later argument reassigns it.
  void assignInLaterArgument() {
    ReceiverReassignedInArgument r = new ReceiverReassignedInArgument();
    r.take(r, r = null);
    // :: error: (dereference.of.nullable)
    r.toString();
  }

  void assignInNestedArgument() {
    ReceiverReassignedInArgument r = new ReceiverReassignedInArgument();
    r.take(String.valueOf(r = null));
    // :: error: (dereference.of.nullable)
    r.toString();
  }

  // The argument assigns a non-null value to the receiver expression.
  void assignNonNullInArgument(@Nullable Object r, Object nn) {
    // :: error: (dereference.of.nullable)
    r.equals(r = nn);
    r.toString();
  }

  // Evaluating the argument does not execute the lambda body, and a side-effect-free method does
  // not execute it either.
  void assignInLambdaBody() {
    // :: error: (dereference.of.nullable)
    f.sefTake((Runnable) () -> f = null);
    f.toString();
  }

  // Evaluating the argument executes the anonymous class's initializer.
  void assignInAnonymousClassInitializer() {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        new Object() {
          {
            f = null;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }
}
