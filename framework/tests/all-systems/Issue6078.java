import java.lang.invoke.MethodHandle;

public class Issue6078 {
  static void call(MethodHandle methodHandle) throws Throwable {
    // The vararg parameter disappears for the below method. It's not clear why this happens.
    methodHandle.invoke();
  }

  void use() {
    foo();
  }

  @SafeVarargs
  private final <T> void foo(T... ts) {}
}
