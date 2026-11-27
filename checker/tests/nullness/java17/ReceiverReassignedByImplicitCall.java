// @below-java17-jdk-skip-test

import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

// An argument that contains a switch expression can implicitly invoke methods: close() for a
// try-with-resources statement, and iterator(), hasNext(), and next() for an enhanced for loop.
// If such a method might reassign the receiver expression, then normal completion of the call
// does not show that the receiver expression is non-null afterward.
public class ReceiverReassignedByImplicitCall {

  @Nullable ReceiverReassignedByImplicitCall f;

  @SideEffectFree
  void take(int i) {}

  class ClearingResource implements AutoCloseable {
    @Override
    public void close() {
      f = null;
    }
  }

  class PureResource implements AutoCloseable {
    @Override
    @SideEffectFree
    public void close() {}
  }

  void resourceCloseClearsReceiver(int tag) {
    ClearingResource resource = new ClearingResource();
    // :: error: (dereference.of.nullable)
    f.take(
        switch (tag) {
          default -> {
            try (resource) {
            }
            yield 0;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  void resourceDeclarationCloseClearsReceiver(int tag) {
    // :: error: (dereference.of.nullable)
    f.take(
        switch (tag) {
          default -> {
            try (ClearingResource resource = new ClearingResource()) {
            }
            yield 0;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  void resourceCloseIsPure(int tag) {
    PureResource resource = new PureResource();
    // :: error: (dereference.of.nullable)
    f.take(
        switch (tag) {
          default -> {
            try (resource) {
            }
            yield 0;
          }
        });
    f.toString();
  }

  class ClearingIterable implements Iterable<Object> {
    @Override
    public Iterator<Object> iterator() {
      f = null;
      return new java.util.ArrayList<Object>().iterator();
    }
  }

  void iteratorClearsReceiver(int tag, ClearingIterable iterable) {
    // :: error: (dereference.of.nullable)
    f.take(
        switch (tag) {
          default -> {
            for (Object ignored : iterable) {}
            yield 0;
          }
        });
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  void arrayLoop(int tag, Object[] array) {
    // :: error: (dereference.of.nullable)
    f.take(
        switch (tag) {
          default -> {
            for (Object ignored : array) {}
            yield 0;
          }
        });
    f.toString();
  }
}
