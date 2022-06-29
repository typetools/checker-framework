abstract class L4879<B> {
  protected L4879(A4879<B> c) {}
}

class A4879<A> {
  static class B4879<T> {
    public B4879() {}

    A4879<T> build() {
      throw new AssertionError();
    }
  }
}

class Issue4879 {
  private final class I4879 extends L4879<Object> {
    private I4879() {
      super(new A4879.B4879<>().build());
    }
  }
}
