import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.qual.DoesNotUnrefineReceiver;

// A call's receiver is evaluated before its arguments.  If an argument reassigns the receiver
// expression, then normal completion of the call does not show that the receiver expression is
// non-null afterward.  ReceiverRefinementInCatch.java contains more such tests.
public class ReceiverReassignedInArgument {

  @Nullable ReceiverReassignedInArgument f;

  void take(@Nullable Object o) {}

  @SideEffectFree
  void sefTake(@Nullable Object o) {}

  void assignInNestedArgument() {
    ReceiverReassignedInArgument r = new ReceiverReassignedInArgument();
    r.take(String.valueOf(r = null));
    // :: error: (dereference.of.nullable)
    r.toString();
  }

  // The receiver is not refined while the arguments are evaluated.
  void dereferenceInArgument(@Nullable ReceiverReassignedInArgument r) {
    // :: error: (dereference.of.nullable)
    r.equals(
        // :: error: (dereference.of.nullable)
        r.hashCode());
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

  void sideEffect() {}

  @DoesNotUnrefineReceiver("allcheckers")
  void doesNotUnrefineReceiver() {}

  @DoesNotUnrefineReceiver("allcheckers")
  void doesNotUnrefineReceiver(@Nullable Object o) {}

  // The method does not unrefine its receiver, so normal completion shows that f is non-null.
  void doesNotUnrefineReceiverField() {
    // :: error: (dereference.of.nullable)
    f.doesNotUnrefineReceiver();
    f.toString();
  }

  void sideEffectField() {
    // :: error: (dereference.of.nullable)
    f.sideEffect();
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  // An argument that reassigns the receiver blocks the refinement, even if the method does not
  // unrefine its receiver.
  void doesNotUnrefineReceiverArgumentReassigns() {
    // :: error: (dereference.of.nullable)
    f.doesNotUnrefineReceiver(f = null);
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  // A later argument reassigns an argument that is passed to a non-null parameter.
  void laterArgumentReassignsArgument(@Nullable Object x) {
    // :: error: (argument)
    take2(x, x = null);
    // :: error: (dereference.of.nullable)
    x.toString();
  }

  void laterArgumentDoesNotReassignArgument(@Nullable Object x, @Nullable Object y) {
    // :: error: (argument)
    take2(x, y = null);
    x.toString();
  }

  void take2(Object nn, @Nullable Object o) {}
}
