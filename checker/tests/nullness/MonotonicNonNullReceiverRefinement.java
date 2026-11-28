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

  int count;

  int tick() {
    return count++;
  }

  void consume(int i) {}

  // An argument with side effects cannot make the field null again.
  void monotonicReceiverEffectfulArgument() {
    // :: error: (dereference.of.nullable)
    mnn.consume(tick());
    mnn.act();
  }

  void monotonicReceiverOfOtherObjectEffectfulArgument(MonotonicNonNullReceiverRefinement other) {
    // :: error: (dereference.of.nullable)
    other.mnn.consume(tick());
    other.mnn.act();
  }

  // An argument that reassigns the receiver of the field access makes the expression denote a
  // different, possibly null, field.
  void monotonicReceiverOfReassignedObject(
      MonotonicNonNullReceiverRefinement other, MonotonicNonNullReceiverRefinement replacement) {
    // :: error: (dereference.of.nullable)
    other.mnn.take(other = replacement);
    // :: error: (dereference.of.nullable)
    other.mnn.act();
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
