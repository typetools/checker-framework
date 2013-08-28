import checkers.nullness.quals.*;

public class SuperCall {

  public static class A {
    public A(@NonNull Object arg) { }
  }

  public static class B extends A {
    public B(@Nullable Object arg) {
      //:: error: (argument.type.incompatible)
      super(arg);
    }
  }

}
