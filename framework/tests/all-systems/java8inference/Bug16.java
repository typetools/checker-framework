package inference;

public class Bug16 {

    public interface Interface<K1, V1> {}

    private static class Implementation<K, V> implements Interface<K, V> {
        Implementation(Interface<? extends K, ? extends V> delegate, Interface<V, K> inverse) {}

        Implementation(Interface<V, K> inverse, int o) {}

        Implementation(Interface<? extends K, ? extends V> delegate) {}

        void test(Interface<V, K> param) {
            Interface<V, K> inverse1 = new Implementation<>(param, this);
            Interface<V, K> inverse2 = new Implementation<>(param);
            Interface<V, K> inverse3 = new Implementation<>(this, 1);
        }
    }
}
