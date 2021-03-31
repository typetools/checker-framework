abstract class Super3022<T> {
  class Wrapper {
    Wrapper(T key) {}
  }
}

class Sub3022<K> extends Super3022<K> {
  void wrap(K key) {
    new Wrapper(key);
  }
}
