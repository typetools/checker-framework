import org.checkerframework.checker.index.qual.Positive;

public class Boilerplate {

  void test() {
    // :: error: (assignment)
    @Positive int a = -1;
  }
}
// a comment
