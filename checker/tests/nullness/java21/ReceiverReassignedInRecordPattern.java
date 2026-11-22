// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

// Matching a record pattern in an argument implicitly invokes the record's component accessors,
// which might reassign the receiver of the call.
public class ReceiverReassignedInRecordPattern {

  static @Nullable ReceiverReassignedInRecordPattern f;

  @SideEffectFree
  void sefAccept(boolean b) {}

  record Clearing(int n) {
    // An accessor that has a side effect.
    public int n() {
      f = null;
      return n;
    }
  }

  record Plain(int n) {
    @Pure
    public int n() {
      return n;
    }
  }

  void accessorReassignsReceiver(Object rec) {
    // :: error: (dereference.of.nullable)
    f.sefAccept(rec instanceof Clearing(int n));
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  void pureAccessor(Object rec) {
    // :: error: (dereference.of.nullable)
    f.sefAccept(rec instanceof Plain(int n));
    f.toString();
  }
}
