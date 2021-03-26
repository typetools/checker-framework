// Test case for https://github.com/typetools/checker-framework/issues/3307

public class Issue3307 extends A<Integer> {
  private int a = 0;

  void bar(int value) {
    if (a != value) {
      a = value;
      foo(value);
    }
  }
}

@SuppressWarnings("unchecked")
abstract class A<B> {
  protected final void foo(B... values) {}
}
