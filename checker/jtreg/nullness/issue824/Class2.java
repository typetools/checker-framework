import org.checkerframework.checker.nullness.qual.Nullable;

public class Class2<X> extends Class1<X> {
    void call(Class1<@Nullable X> class1, Gen<@Nullable X> gen) {
        class1.methodTypeParam(null);
        class1.classTypeParam(null);

        class1.wildcardExtends(gen);
        class1.wildcardSuper(gen);
    }

    @Override
    public <T> T methodTypeParam(T t) {
        return super.methodTypeParam(t);
    }
}
