import checkers.quals.*;
import checkers.util.test.*;

// Test case for Issue 140:
// http://code.google.com/p/checker-framework/issues/detail?id=140
class GenericTest9 {
    // Make sure that substitutions on classes work correctly

    class C<X, Y extends X> {}
    
    C<Object, Entry<String>> f = new C<Object, Entry<String>>();

    interface Entry<S> {}

    <V> void testclass() {
        //:: error: (type.argument.type.incompatible)
        C<@Odd Object, Entry<V>> c1 = new C<@Odd Object, Entry<V>>();
        C<@Odd Object, @Odd Entry<V>> c2 = new C<@Odd Object, @Odd Entry<V>>();
    }

    // Make sure that substitutions on method type variables work correctly

    interface Ordering1<T> {
        <U extends T> U sort(U iterable);
    }

    <V> void test(Ordering1<Entry<?>> o, Entry<V> e) {
        Entry<V> e1 = o.sort(e);
        Entry<V> e2 = o.<Entry<V>>sort(e);
    }

    interface Ordering2<T extends @Odd Object> {
        <U extends T> U sort(U iterable);
    }

    <V> void test(Ordering2<@Odd Entry<?>> ord, Entry<V> e, @Odd Entry<V> o) {
        //:: error: (type.argument.type.incompatible)
        Entry<V> e1 = ord.sort(e);
        //:: error: (type.argument.type.incompatible)
        Entry<V> e2 = ord.<Entry<V>>sort(e);
        Entry<V> e3 = ord.sort(o);
        Entry<V> e4 = ord.<@Odd Entry<V>>sort(o);
    }

    /* TODO: @Odd doesn't use @Target TYPE_PARAMETER, so this doesn't work.
     * If I add it, the AnnotatedVoidMethod test fails, even though it shouldn't
     * -> langtools bug for not forbidding TYPE_PARAMETER on void.
    interface Ordering3<@Odd T> {
        <U extends T> U sort(U iterable);
    }

    <V> void test(Ordering3<@Odd Entry<?>> o, @Odd Entry<V> e) {
        Entry<V> e1 = o.sort(e);
        Entry<V> e2 = o.<@Odd Entry<V>>sort(e);
    }
    */
}
