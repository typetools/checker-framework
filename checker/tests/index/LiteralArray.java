// test case for issue #67:
// https://github.com/kelloggm/checker-framework/issues/67

import org.checkerframework.checker.index.qual.*;

public class LiteralArray {

  private static final String[] timeFormat = {
    ("#.#"), ("#.#"), ("#.#"), ("#.#"), ("#.#"),
  };

  public String format() {
    return format(1);
  }

  public String format(@IndexFor("LiteralArray.timeFormat") int digits) {
    return "";
  }
}
