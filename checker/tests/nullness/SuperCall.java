import org.checkerframework.checker.nullness.qual.*;

public class SuperCall {

  public static class A {
    public A(@NonNull Object arg) {}
  }

  public static class B extends A {
    public B(@Nullable Object arg) {
      // :: error: (argument)
      super(arg);
    }
  }
}
