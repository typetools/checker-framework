import org.checkerframework.common.value.qual.IntRange;

public class CharacterRadix {

  void f() {
    for (@IntRange(from = 1, to = 16) int i = 1; i <= 15; i++) {
      // empty body
    }
  }

  void g() {
    for (@IntRange(from = 0, to = 0) int i = 0; i > 15; i--) {
      // unreachable, but should not crash analysis
    }
  }

  void h(boolean ready) {
    for (@IntRange(from = 1, to = 16) int i = 1; i <= 15 && ready; i++) {
      // composite condition should get the same refinement
    }
  }
}
