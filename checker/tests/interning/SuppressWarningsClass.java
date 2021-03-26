import org.checkerframework.checker.interning.qual.*;

@SuppressWarnings("interning")
public class SuppressWarningsClass {

  public static void myMethod() {

    @Interned String s = new String();
  }
}
