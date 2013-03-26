public class GenericsConstructor {
  class Test {
      <T> Test(T param) {}
      <T1, T2 extends T1> Test(T1 p1, T2 p2) {}
  }

  void call() {
      new Test("Ha!");
      new <String>Test("Ha!");
      new Test(new Object());
      
      // new <String, String>Test("Hi", "Ho");
  }
}
