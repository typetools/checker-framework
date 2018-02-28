// Test case for Issue #807:
// https://github.com/typetools/checker-framework/issues/807

import java.util.function.Consumer;

class Issue807 {

    class MyEntry<K, V> {
        MyEntry(MyEntry<? extends K, ? extends V> e) {}
    }

    <K, V> Consumer<MyEntry<K, V>> entryConsumer(Consumer<? super MyEntry<K, V>> action) {
        // The "new MyEntry" isn't a subtype of "? super MyEntry" in
        // most type systems. Suppress that error, as it's not the
        // point of this test.
        @SuppressWarnings("")
        Consumer<MyEntry<K, V>> res = e -> action.accept(new MyEntry<>(e));
        return res;
    }
}
