public class Issue6867 {
  interface A<T extends Number> {}

  static class B<T extends A<? super Integer>> {}

  void main(B<A<Number>> b) {
    @SuppressWarnings("super.wildcard") // This is a true positive.
    // This code is rejected by Eclipse and IntelliJ's presentation compiler. It's a bug in javac.
    // https://bugs.openjdk.org/browse/JDK-8054309
    B<A<? super Number>> x = b;
  }
}
