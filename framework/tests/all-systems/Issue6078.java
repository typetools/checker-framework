import java.lang.invoke.MethodHandle;

public class Issue6078 {
  static void call(MethodHandle methodHandle) throws Throwable {
    methodHandle.invoke();
  }

  void use() {
    foo();
  }

  @SafeVarargs
  private final <T> void foo(T... ts) {}
}
