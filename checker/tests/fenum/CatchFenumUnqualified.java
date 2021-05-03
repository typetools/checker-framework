import org.checkerframework.checker.fenum.qual.Fenum;

public class CatchFenumUnqualified {
  void method() {
    try {
    } catch (
        // :: error: (exception.parameter)
        @Fenum("A") RuntimeException e) {

    }
    try {
      // :: error: (exception.parameter)
    } catch (@Fenum("A") NullPointerException | @Fenum("A") ArrayIndexOutOfBoundsException e) {

    }
  }
}
