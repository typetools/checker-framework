import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

// A switch expression in an argument can contain statements that implicitly invoke methods, which
// might reassign the receiver expression.  See also ReceiverReassignedInArgument.java.
public class ReceiverReassignedInSwitchExpression {

  @Nullable ReceiverReassignedInSwitchExpression f;

  @SideEffectFree
  void sefTake(@Nullable Object o) {}

  interface Resource extends AutoCloseable {
    @Override
    void close();
  }

  interface SefResource extends AutoCloseable {
    @Override
    @SideEffectFree
    void close();
  }

  // The try-with-resources statement implicitly invokes close(), which might reassign f.
  void tryWithResourcesInArgument(int n, Resource resource) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (n) {
          default -> {
            try (resource) {}
            yield 0;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  void sefTryWithResourcesInArgument(int n, SefResource resource) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (n) {
          default -> {
            try (resource) {}
            yield 0;
          }
        });
    f.toString();
  }

  // A try statement without resources invokes no method.
  void tryInArgument(int n) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (n) {
          default -> {
            try {
            } finally {
            }
            yield 0;
          }
        });
    f.toString();
  }

  // The enhanced for loop implicitly invokes iterator(), which might reassign f.
  void enhancedForOverIterableInArgument(int n, Iterable<Object> iterable) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (n) {
          default -> {
            for (Object x : iterable) {}
            yield 0;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  // An enhanced for loop over an array invokes no method.
  void enhancedForOverArrayInArgument(int n, Object[] array) {
    // :: error: (dereference.of.nullable)
    f.sefTake(
        switch (n) {
          default -> {
            for (Object x : array) {}
            yield 0;
          }
        });
    f.toString();
  }
}
