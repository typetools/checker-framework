import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

// A switch expression in an argument can contain statements that implicitly invoke methods, which
// might reassign the receiver expression after the receiver is evaluated.  Then normal completion
// of the call does not show that the receiver expression is non-null afterward.
// See also ReceiverReassignedInArgument.java.
public class ReceiverReassignedInSwitchArgument {

  @Nullable ReceiverReassignedInSwitchArgument f;

  @SideEffectFree
  void sefTake(int i) {}

  // An enhanced for loop over an Iterable implicitly invokes iterator(), hasNext(), and next(),
  // which might reassign f.
  void enhancedForOverIterable(List<Object> items, int i) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (i) {
          default -> {
            for (Object unused : items) {}
            yield 0;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  // An enhanced for loop over an array invokes no method.
  void enhancedForOverArray(Object[] items, int i) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (i) {
          default -> {
            for (Object unused : items) {}
            yield 0;
          }
        });
    f.toString();
  }

  // A close() method that reassigns the field f.
  class Resource implements AutoCloseable {
    @Override
    public void close() {
      f = null;
    }
  }

  class SefResource implements AutoCloseable {
    @Override
    @SideEffectFree
    public void close() {}
  }

  // A try-with-resources statement implicitly invokes Resource.close(), which reassigns f.
  void tryWithResources(Resource r, int i) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (i) {
          default -> {
            try (Resource r2 = r) {
            }
            yield 0;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  // Likewise for a resource that is an expression rather than a variable declaration.
  void tryWithResourcesExpression(Resource r, int i) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (i) {
          default -> {
            try (r) {
            }
            yield 0;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  // A try-with-resources statement whose close() is side-effect-free.
  void sefTryWithResources(SefResource r, int i) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (i) {
          default -> {
            try (SefResource r2 = r) {
            }
            yield 0;
          }
        });
    f.toString();
  }
}
