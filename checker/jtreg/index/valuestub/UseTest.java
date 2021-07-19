package valuestub;

import org.checkerframework.checker.index.qual.IndexOrHigh;

public class UseTest {
  void test(Test t) {
    @IndexOrHigh("t") int x = t.length();
  }
}
