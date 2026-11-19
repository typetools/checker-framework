import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

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

  @Target(ElementType.TYPE_USE)
  @interface TypeUseAnno {
    int value();
  }

  // An annotation's element-value pair, such as `value = 1`, is not an assignment.
  void annotationInArgument(Object r, @Nullable Object other) {
    r.equals((@TypeUseAnno(value = 1) Object) other);
    r.toString();
  }
}
