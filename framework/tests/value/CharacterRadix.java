import org.checkerframework.common.value.qual.IntRange;

public class CharacterRadix {

  void f() {
    for (@IntRange(from = 1, to = 16) int i = 1; i <= 15; i++) {
      // empty body
    }
  }
}
