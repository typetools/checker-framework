@SuppressWarnings("all") // Just check for crashes.
public class Issue6825 {
  static class ClassA<T extends Number> {}

  public static boolean flag;
  ClassA<? super Number> f;

  void method(Number n) {
    var y = flag ? f : new ClassA<Number>();
    var x = flag ? this.f : new ClassA<Number>();
  }
}
