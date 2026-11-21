import java.util.List;
import org.checkerframework.checker.interning.qual.Interned;

// An enhanced for loop that boxes or unboxes its elements.  Every primitive is interned, but a
// boxed value need not be, except for a boxed boolean or byte.
public class EnhancedForBoxing {
  void boxInts(int[] ints) {
    for (Integer x : ints) {
      // :: error: [assignment]
      @Interned Integer y = x;
    }
    for (Object x : ints) {
      // :: error: [assignment]
      @Interned Object y = x;
    }
  }

  void boxBytesAndBooleans(byte[] bytes, boolean[] bools) {
    for (Byte x : bytes) {
      @Interned Byte y = x;
    }
    for (Boolean x : bools) {
      @Interned Boolean y = x;
    }
  }

  // Unboxing an element yields an interned primitive.
  void unboxList(List<Integer> list) {
    for (int x : list) {
      @Interned int y = x;
    }
  }

  void unboxWildcardList(List<? extends Integer> list) {
    for (int x : list) {
      @Interned int y = x;
    }
    for (long x : list) {
      @Interned long y = x;
    }
  }
}
