import java.lang.invoke.MethodHandle;

public class Issue6078 {
  @SuppressWarnings("nullness:argument") // true positive.
  static void call(MethodHandle methodHandle, Object[] array) throws Throwable {
    // The vararg parameter disappears for the below method. It's some sort of bug in javac.
    // It is worked around in TreeUtils.isVarargsCall().
    methodHandle.invoke();
    // The vararg parameter does not disappaer for these method calls.
    methodHandle.invoke("");
    methodHandle.invoke(array);
    methodHandle.invoke(null);
  }

  void use() {
    foo();
  }

  @SafeVarargs
  private final <T> void foo(T... ts) {}
}
