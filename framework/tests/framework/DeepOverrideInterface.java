import org.checkerframework.framework.testchecker.util.*;

public class DeepOverrideInterface {

  public static interface I {
    @Odd String interfaceMethod();
  }

  public abstract static class A {
    public abstract @Odd String abstractMethod();
  }

  public abstract static class B extends A implements I {}

  public static class C extends B {
    // :: error: (override.return.invalid)
    public String interfaceMethod() {
      return "";
    }

    public @Odd String abstractMethod() {
      return null;
    }
  }
}
