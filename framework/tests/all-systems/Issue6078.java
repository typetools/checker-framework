public class Issue6078 {
  //    static void call(MethodHandle methodHandle) throws Throwable {
  //    methodHandle.invoke();
  //  }
  void use() {
    foo();
  }

  <T> void foo(T... ts) {}
}
