import org.checkerframework.framework.testchecker.util.*;

// Test case for Issue 140:
// https://github.com/typetools/checker-framework/issues/140
public class GenericTest9 {
  // Make sure that substitutions on classes work correctly

  class C<X, Y extends X> {}

  C<Object, MyEntry<String>> f = new C<>();

  interface MyEntry<S> {}

  <V> void testclass() {
    // :: error: (type.argument)
    C<@Odd Object, MyEntry<V>> c1 = new C<>();
    C<@Odd Object, @Odd MyEntry<V>> c2 = new C<>();
  }

  // Make sure that substitutions on method type variables work correctly

  interface Ordering1<T> {
    <U extends T> U sort(U iterable);
  }

  <V> void test(Ordering1<MyEntry<?>> o, MyEntry<V> e) {
    MyEntry<V> e1 = o.sort(e);
    MyEntry<V> e2 = o.<MyEntry<V>>sort(e);
  }

  interface Ordering2<T extends @Odd Object> {
    <U extends T> U sort(U iterable);
  }

  <V> void test(Ordering2<@Odd MyEntry<?>> ord, MyEntry<V> e, @Odd MyEntry<V> o) {
    // :: error: (type.argument)
    MyEntry<V> e1 = ord.sort(e);
    // :: error: (type.argument)
    MyEntry<V> e2 = ord.<MyEntry<V>>sort(e);
    MyEntry<V> e3 = ord.sort(o);
    MyEntry<V> e4 = ord.<@Odd MyEntry<V>>sort(o);
  }

  interface Ordering3<@Odd T> {
    <U extends T> U sort(U iterable);
  }

  <V> void test(Ordering3<@Odd MyEntry<?>> o, @Odd MyEntry<V> e) {
    MyEntry<V> e1 = o.sort(e);
    MyEntry<V> e2 = o.<@Odd MyEntry<V>>sort(e);
    // :: error: (type.argument) :: error: (argument)
    MyEntry<V> e3 = o.<@Even MyEntry<V>>sort(e);
  }

  interface Comparator4<T> {}

  interface Ordering4<T> extends Comparator4<T> {
    <S extends T> Ordering4<S> reverse();
  }

  <T> Ordering4<T> from4(Comparator4<T> comparator) {
    return null;
  }

  <E> Comparator4<? super E> reverseComparator4(Comparator4<? super E> comparator) {
    // Making the method type argument explicit:
    //   from4(comparator).<E>reverse();
    // has the same result.
    from4(comparator).<E>reverse();
    return from4(comparator).reverse();
  }
}
