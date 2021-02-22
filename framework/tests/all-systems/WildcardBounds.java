import java.io.Serializable;

public class WildcardBounds {
    class BoundedGeneric<B extends Cloneable> {}

    class BoundedGeneric2<B extends Number> {}

    class BoundedGeneric3<B extends Number & Cloneable> {}

    void use() {
        BoundedGeneric<? extends Serializable> a;
        BoundedGeneric<? extends Serializable> b;
        BoundedGeneric<? extends Serializable> c;
    }
}
