package valuestub;

import org.checkerframework.checker.index.qual.LengthOf;

@SuppressWarnings("index")
public class Test {
  public @LengthOf("this") int length() {
    return 1;
  }
}
