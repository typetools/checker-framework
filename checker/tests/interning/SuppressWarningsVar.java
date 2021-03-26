import org.checkerframework.checker.interning.qual.*;

public class SuppressWarningsVar {

  public static void myMethod() {

    @SuppressWarnings("interning")
    @Interned String s = new String();
  }
}
