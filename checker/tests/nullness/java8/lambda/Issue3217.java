import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

public class Issue3217<ModelA, ModelB, Value> {
    private final Function<Function<ModelA, @Nullable Value>, Function<ModelB, @Nullable Value>>
            proxyFunction;

    public Issue3217(
            Function<Function<ModelA, @Nullable Value>, Function<ModelB, @Nullable Value>>
                    proxyFunction) {
        this.proxyFunction = proxyFunction;
    }
}

class SubClass<A, V> extends Issue3217<A, A, V> {
    public SubClass() {
        super(x -> x);
        Function<Function<A, @Nullable V>, Function<A, @Nullable V>> p = y -> y;
    }
}
