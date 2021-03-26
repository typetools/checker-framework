import org.checkerframework.checker.index.qual.Positive;

public class Boilerplate {

  void test() {
    // :: error: (assignment.type.incompatible)
    @Positive int a = -1;
  }
}
// a comment
