import tests.util.*;

// Test case for Issue 135:
// http://code.google.com/p/checker-framework/issues/detail?id=135
// Method type argument substitution needs to consider arrays correctly.
public class GenericTest5 {
  interface Foo { <T> T[] id(T[] a); }
 
  public <U> void test(Foo foo, U[] a) {
    U[] array = foo.id(a);
  }
  
  public <T> void test1(Foo foo, T[] a) {
    T[] array = foo.id(a);
  }

  public <S extends @Odd Object> void test2(Foo foo, S[] a) {
    S[] array = foo.id(a);
  }

  public <S extends @Odd Object, T extends S> void test3(Foo foo, T[] a) {
    T[] array = foo.id(a);
  }
}
