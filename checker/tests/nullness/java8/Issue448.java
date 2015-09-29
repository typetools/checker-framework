// Test case for issue 448:
// https://github.com/typetools/checker-framework/issues/448
//@skip-test
import java.util.Arrays;

enum Issue448 {
  ONE;

  static Issue448 getFor(int index) {
    return Arrays.stream(values())
        .filter(key -> key.ordinal() == index)
        .findFirst()
        .orElseThrow(IllegalArgumentException::new);
  }
}
