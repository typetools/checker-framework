// Test case for https://github.com/typetools/checker-framework/issues/8325

import java.util.BitSet;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue8325 {

  @Nullable Object field;

  void clearAll(BitSet bits) {
    bits.clear();
  }

  void clearOne(BitSet bits) {
    bits.clear(0);
  }

  void clearRange(BitSet bits) {
    bits.clear(0, 10);
  }

  void refinementPreserved(BitSet bits) {
    if (field != null) {
      bits.clear();
      field.toString();
    }
  }
}
