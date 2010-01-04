import checkers.nullness.quals.*;
import java.util.*;

class Utils {

    <A extends @Nullable Object>
    void test(List<? super A> list, A object) {
        list.add(object);
    }

    interface Consumer<A extends @Nullable Object> {
        public void consume(A object);
    }

    public static <A extends @Nullable Object>
    Consumer<A> cast(final Consumer<? super A> consumer) {
        return new Consumer<A>() {
            @Override public void consume(A object) {
                consumer.consume(object);
            }
        };
    }

    public static <A extends @Nullable Object> Consumer<A>
    getConsumer(Consumer<@Nullable Object> nullConsumer) {
        return Utils.<A>cast(nullConsumer);
    }

}
