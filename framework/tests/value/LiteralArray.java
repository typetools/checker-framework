import org.checkerframework.common.value.qual.*;

public class LiteralArray {

  private static final String[] timeFormat = {
    ("#.#"), ("#.#"), ("#.#"), ("#.#"), ("#.#"),
  };

  public void test() {
    String @ArrayLen(5) [] array = timeFormat;
  }
}
