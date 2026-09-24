import java.util.Iterator;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.IteratorPolyMod;
import org.checkerframework.checker.modifiability.qual.MaybeShrinkable;
import org.checkerframework.checker.modifiability.qual.Shrinkable;
import org.checkerframework.checker.modifiability.qual.Unshrinkable;

/**
 * The result of an iterator method whose receiver is a type variable is refined from the upper
 * bound of the type variable, just as it would be from a receiver of the bound's type.
 */
public class IteratorTypeVariableReceiverTest {

  <L extends @Shrinkable @IteratorPolyMod List<String>> void positive(L list) {
    Iterator<String> it = list.iterator();
    it.remove();
    @Shrinkable Iterator<String> shrinkable = list.iterator();
  }

  void positiveDeclared(@Shrinkable @IteratorPolyMod List<String> list) {
    Iterator<String> it = list.iterator();
    it.remove();
  }

  <L extends @Unshrinkable List<String>> void negative(L list) {
    @Unshrinkable Iterator<String> it = list.iterator();
  }

  // The receiver can shrink, but it does not promise that its iterator can.
  <L extends @Shrinkable List<String>> void notIteratorPolyMod(L list) {
    @MaybeShrinkable Iterator<String> it = list.iterator();
    // :: error: [assignment]
    @Shrinkable Iterator<String> shrinkable = list.iterator();
  }
}
