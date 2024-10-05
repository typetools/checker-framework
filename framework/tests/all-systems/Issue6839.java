public class Issue6839<Y> {
  Issue6839(Y x) {}

  class Inner<T> extends Issue6839<T> {
    Inner(T x) {
      super(x);
    }
  }

  void main() {
    var x = new Issue6839<>(1).new Inner<>(1);
  }
}
