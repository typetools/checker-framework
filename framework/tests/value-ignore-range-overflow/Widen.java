package value;

import org.checkerframework.common.value.qual.*;

public class Widen {
  public class Loops {
    void test(int c, int max) {
      int decexp = 0;
      int seendot = 0;
      int i = 0;
      while (true) {
        if (c == '.' && seendot == 0) {
          seendot = 1;
        } else if ('0' <= c && c <= '9') {
          decexp += seendot;
        }

        if (max < i++) {
          break;
        }
      }
    }
  }
}
