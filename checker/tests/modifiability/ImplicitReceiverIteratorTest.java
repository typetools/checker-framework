import java.util.AbstractCollection;
import java.util.Iterator;
import org.checkerframework.checker.modifiability.qual.MaybeShrinkable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unshrinkable;

/**
 * An iterator method that is called on the implicit {@code this} receiver is refined from the
 * receiver type of the enclosing method, just as it would be from an explicit receiver.
 */
public class ImplicitReceiverIteratorTest extends AbstractCollection<String> {

  @Override
  public @Shrinkable Iterator<String> iterator() {
    throw new Error("not implemented");
  }

  @Override
  public int size() {
    return 1;
  }

  void negativeReceiver(@Unshrinkable ImplicitReceiverIteratorTest this) {
    // The iterator of a collection that cannot shrink cannot shrink either, even though the
    // declaration says that it can.
    @Unshrinkable Iterator<String> it = iterator();
  }

  void unknownReceiver(@MaybeShrinkable ImplicitReceiverIteratorTest this) {
    @MaybeShrinkable Iterator<String> it = iterator();
    // :: error: [assignment]
    @Shrinkable Iterator<String> shrinkable = iterator();
  }

  void explicitReceiver(@Unshrinkable ImplicitReceiverIteratorTest other) {
    @Unshrinkable Iterator<String> it = other.iterator();
  }
}
