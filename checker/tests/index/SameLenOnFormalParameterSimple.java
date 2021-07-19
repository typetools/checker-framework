// Test case for issue #2434: http://tinyurl.com/cfissue/2434

import org.checkerframework.checker.index.qual.SameLen;

public class SameLenOnFormalParameterSimple {
  public void requiresSameLen1(String x1, @SameLen("#1") String y1) {}

  public void m1(@SameLen("#2") String a1, String b1) {
    requiresSameLen1(a1, b1);
  }
}
