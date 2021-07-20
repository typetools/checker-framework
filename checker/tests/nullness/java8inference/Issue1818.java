import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class Issue1818 {
    void f() {
        Consumer<List<?>> c = values -> values.forEach(value -> g(value));
    }

    void g(@Nullable Object o) {}
}
