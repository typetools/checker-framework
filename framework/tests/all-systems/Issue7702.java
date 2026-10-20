import java.lang.invoke.MethodHandle;

public class Issue7702 {
  interface Foo {
    <T extends Bar> T passthru(T t);
  }

  interface Bar {}

  void run(MethodHandle handle, Object o, Foo foo, Bar bar) throws Throwable {
    handle.invokeExact(o, foo.passthru(bar));
  }
}
