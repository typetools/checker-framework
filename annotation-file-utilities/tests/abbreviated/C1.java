public class C1<T extends Object> {
  int values = 0;

  public <A extends Object> C1<A> foo(A a) {
    return null;
  }
}
