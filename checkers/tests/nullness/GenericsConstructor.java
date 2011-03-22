public class GenericsConstructor {
  class Test {
    <T> Test(T param) {}
  }

  void call() {
      new Test("Ha!");
      new <String>Test("Ha!");
      new Test(new Object());
  }
}
