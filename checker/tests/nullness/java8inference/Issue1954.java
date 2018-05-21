import java.util.function.*;
import java.util.stream.*;
import org.checkerframework.checker.nullness.qual.*;

// @skip-test
public class Issue1954 {
    public interface Getter<R> {
        R get();
    }

    public interface NullStringGetter extends Getter<@Nullable String> {}

    public <T, R> Getter<R> transform(Function<Stream<T>, R> fn, Getter<T> getter) {
        return () -> fn.apply(Stream.of(getter.get()));
    }

    public static <T> @Nullable T fn(Stream<T> arg) {
        return arg.findFirst().orElse(null);
    }

    public void doo() {
        NullStringGetter nullStringGetter = () -> null;
        // :: error: type inference failed.
        transform(Issue1954::fn, nullStringGetter).get();
    }
}
