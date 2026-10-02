import java.util.AbstractCollection;
import java.util.Iterator;
import org.checkerframework.checker.modifiability.qual.MaybeShrinkable;
import org.checkerframework.checker.modifiability.qual.PolyShrinkable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unshrinkable;

/**
 * An iterator method whose result is declared polymorphic relates the iterator to the receiver, so
 * its result is the qualifier that polymorphic resolution gives it, even though the receiver is not
 * {@code @IteratorPolyMod}.
 */
public class PolyIteratorResultTest extends AbstractCollection<String> {

  // `AbstractCollection.iterator()` declares its receiver `@MaybeModifiable`, so a polymorphic
  // receiver is a stronger requirement, but the result must still be resolved polymorphically.
  @Override
  // :: error: [override.receiver]
  public @PolyShrinkable Iterator<String> iterator(@PolyShrinkable PolyIteratorResultTest this) {
    throw new Error("not implemented");
  }

  @Override
  public int size() {
    return 1;
  }

  void shrinkableReceiver(@Shrinkable PolyIteratorResultTest c) {
    @Shrinkable Iterator<String> it = c.iterator();
    it.remove();
  }

  void unshrinkableReceiver(@Unshrinkable PolyIteratorResultTest c) {
    @Unshrinkable Iterator<String> it = c.iterator();
  }

  void unknownReceiver(@MaybeShrinkable PolyIteratorResultTest c) {
    @MaybeShrinkable Iterator<String> it = c.iterator();
    // :: error: [assignment]
    @Shrinkable Iterator<String> shrinkable = c.iterator();
  }
}
