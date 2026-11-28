import java.util.List;
import org.checkerframework.checker.interning.qual.Interned;

// Boxing the element of an enhanced for loop yields a value that is not interned, except for
// boolean and byte, for which boxing returns a cached object.
public class EnhancedForBoxing {
  void intArray(int[] ints) {
    for (Integer x : ints) {
      // :: error: [assignment]
      @Interned Integer y = x;
    }
    for (Integer x : new int[] {1000}) {
      // :: error: [assignment]
      @Interned Integer y = x;
    }
  }

  void booleanAndByteArrays(boolean[] bools, byte[] bytes) {
    for (Boolean x : bools) {
      @Interned Boolean y = x;
    }
    for (Byte x : bytes) {
      @Interned Byte y = x;
    }
  }

  void unboxing(List<Integer> list) {
    for (int x : list) {
      @Interned int y = x;
    }
  }
}
