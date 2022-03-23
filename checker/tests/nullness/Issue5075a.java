import org.checkerframework.checker.nullness.qual.Nullable;

class Issue5075a {
    class AExpl<V extends @Nullable Object> {
        I<V> i1() {
            // Type arguments aren't inferred correctly, test with #979
            // :: error: (return.type.incompatible) :: error: (argument.type.incompatible)
            return new BExpl<>(this);
        }

        I<V> i2() {
            return new BExpl<Object, V>(this);
        }
    }

    class BExpl<K, V> implements I<V> {
        BExpl(AExpl<V> a) {}
    }

    class AImpl<V> {
        I<V> i() {
            return new BImpl<>(this);
        }
    }

    class BImpl<K, V> implements I<V> {
        BImpl(AImpl<V> a) {}
    }

    interface I<V> {}
}
