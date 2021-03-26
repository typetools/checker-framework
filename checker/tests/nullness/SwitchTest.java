import org.checkerframework.checker.nullness.qual.*;

public class SwitchTest {
  public static void main(String[] args) {
    // :: error: (switching.nullable)
    switch (getNbl()) {
      case X:
        System.out.println("X");
        break;
      default:
        System.out.println("default");
    }
  }

  public static void goodUse() {
    switch (getNN()) {
      case X:
        System.out.println("X");
        break;
      default:
        System.out.println("default");
    }
  }

  public static @Nullable A getNbl() {
    return null;
  }

  public static A getNN() {
    return A.X;
  }

  public static enum A {
    X
  }
}
