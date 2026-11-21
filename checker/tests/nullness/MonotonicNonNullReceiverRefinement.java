import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// After a call completes normally, a @MonotonicNonNull receiver or argument remains non-null even
// though the called method has side effects, because no code can make the field null again.
public class MonotonicNonNullReceiverRefinement {

  @MonotonicNonNull MonotonicNonNullReceiverRefinement mnn;

  @Nullable MonotonicNonNullReceiverRefinement nble;

  void act() {}

  void take(Object o) {}

  void monotonicReceiver() {
    // :: error: (dereference.of.nullable)
    mnn.act();
    mnn.act();
  }

  void monotonicReceiverOfOtherObject(MonotonicNonNullReceiverRefinement other) {
    // :: error: (dereference.of.nullable)
    other.mnn.act();
    other.mnn.act();
  }

  void monotonicArgument() {
    // :: error: (argument)
    take(mnn);
    mnn.act();
  }

  // A side-effecting call can set an ordinary field to null.
  void nullableReceiver() {
    // :: error: (dereference.of.nullable)
    nble.act();
    // :: error: (dereference.of.nullable)
    nble.act();
  }

  MonotonicNonNullReceiverRefinement nn = new MonotonicNonNullReceiverRefinement();

  // The call might reassign the receiver of the field access, so that the expression denotes a
  // different, possibly null, field.
  void unstableReceiver() {
    // :: error: (dereference.of.nullable)
    nn.mnn.act();
    // :: error: (dereference.of.nullable)
    nn.mnn.act();
  }
}
