import org.checkerframework.checker.nullness.qual.*;

import java.lang.annotation.Annotation;
import java.util.*;
import java.lang.annotation.*;

class Utils {

    <A extends @Nullable Object>
    void test(List<? super A> list, A object) {
        list.add(object);
    }

    interface Consumer<A extends @Nullable Object> {
        public void consume(A object);
    }

    public static <A extends @Nullable Object>
    Consumer<A> cast(final Consumer<@Nullable ? super A> consumer) {
        return new Consumer<A>() {
            @Override public void consume(A object) {
                consumer.consume(object);
            }
        };
    }

    public static <A extends @Nullable Object> Consumer<A> getConsumer(Consumer<@Nullable Object> nullConsumer) {
        return Utils.<A>cast(nullConsumer);
    }

    Map<String, Set<?>> mss = new HashMap<>();

    Set<Class<? extends Annotation>> foo() {
        Set<Class<? extends Annotation>> l = new HashSet<>(this.foo());
        return l;
    }
}
