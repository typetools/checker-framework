import org.checkerframework.checker.nullness.qual.*;

public class SuppressWarningsTest {

  @SuppressWarnings("all")
  void test() {
    String a = null;
    a.toString();
  }
}
