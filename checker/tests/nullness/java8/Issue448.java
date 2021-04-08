// Test case for issue 448:
// https://github.com/typetools/checker-framework/issues/448

import java.util.Arrays;

enum Issue448 {
  ONE;

  void method() {
    Arrays.stream(values()).filter(key -> true);
  }
}
