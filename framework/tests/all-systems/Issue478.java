// Test case for Issue 478:
// https://github.com/typetools/checker-framework/issues/478

import java.io.Serializable;
import java.util.Comparator;

public class Issue478 {
  public static Comparator<Object> allTheSame() {
    return (Comparator<Object> & Serializable) (c1, c2) -> 0;
  }
}
