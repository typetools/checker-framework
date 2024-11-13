import org.checkerframework.checker.signature.qual.*;

public class Concatenation {

  @Identifier String m(@Identifier String s, int i) {
    return s + i;
  }
}
