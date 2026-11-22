import java.util.Arrays;
import java.util.Iterator;
import org.checkerframework.checker.interning.qual.Interned;

// An array initializer that boxes or unboxes its elements.
public class ArrayInitializerBoxing {
  void box(int i, long l, double d) {
    Integer[] is = new Integer[] {i, i - 1, i};
    Long[] ls = {l, l + 1};
    Object[] os = new Object[] {i, l, d};
    Iterator<Integer> itor = Arrays.stream(new Integer[] {i, i - 1}).iterator();
    // :: error: [assignment]
    @Interned Integer[] interned = new Integer[] {i};
  }

  void boxBooleanAndByte(boolean bool, byte b) {
    @Interned Boolean[] bools = new Boolean[] {bool, bool};
    @Interned Byte[] bytes = new Byte[] {b, b};
  }

  void unbox(Integer i, Long l) {
    @Interned int[] is = new int[] {i, i};
    @Interned long[] ls = {l, l};
  }

  // The initializer's type is a type variable whose bound is a boxed primitive.  It unboxes to
  // int, which is then widened to long.
  <T extends Integer> void unboxTypeVariable(T t) {
    @Interned long[] ls = new long[] {t, t};
  }
}
