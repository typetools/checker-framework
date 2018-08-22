// Test case for Issue #2088:
// https://github.com/typetools/checker-framework/issues/2088

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@SuppressWarnings({"unchecked", ""}) // Check for crashes only
abstract class Issue2088 {

    interface A<K extends Comparable<K>> {}

    interface B extends A<Long> {}

    interface C<P extends B, E extends B> {
        interface F<T extends C<?, ?>> {}
    }

    interface D {}

    static class Key<T> {
        static Key<?> get(Type type) {
            return null;
        }
    }

    abstract ParameterizedType n(Type o, Class<?> r, Type... a);

    <X extends B, Y extends C<?, X>, Z extends Y> void f(Class<Y> c) {
        Key<C.F<Z>> f = (Key<C.F<Z>>) Key.get(n(C.class, C.F.class, c));
    }
}
