import org.checkerframework.checker.nullness.qual.*;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Utils {

    <A extends @Nullable Object> void test(List<? super A> list, A object) {
        list.add(object);
    }

    interface Consumer<A extends @Nullable Object> {
        public void consume(A object);
    }

    public static <A extends @Nullable Object> Consumer<A> cast(
            final Consumer<@Nullable ? super A> consumer) {
        return new Consumer<A>() {
            @Override
            public void consume(A object) {
                consumer.consume(object);
            }
        };
    }

    public static <A extends @Nullable Object> Consumer<A> getConsumer(
            Consumer<@Nullable Object> nullConsumer) {
        return Utils.<A>cast(nullConsumer);
    }

    Map<String, Set<?>> mss = new HashMap<>();

    Set<Class<? extends Annotation>> foo() {
        Set<Class<? extends Annotation>> l = new HashSet<>(this.foo());
        return l;
    }
}

class MyGeneric<@NonNull T extends @Nullable Number> {}

class UseMyGeneric {
    MyGeneric<?> wildcardUnbounded = new MyGeneric<>();

    // :: error: (assignment.type.incompatible)
    MyGeneric<? extends @NonNull Object> wildcardOutsideUB = wildcardUnbounded;
    MyGeneric<? extends @NonNull Number> wildcardInsideUB = wildcardOutsideUB;
    // :: error: (assignment.type.incompatible)
    MyGeneric<? extends @NonNull Number> wildcardInsideUB2 = wildcardUnbounded;

    MyGeneric<? extends @Nullable Number> wildcardInsideUBNullable = wildcardOutsideUB;
}

class MyGenericExactBounds<@NonNull T extends @NonNull Number> {}

class UseMyGenericExactBounds {
    MyGenericExactBounds<? extends @Nullable Object> wildcardOutsideUBError =
            new MyGenericExactBounds<>();
    MyGenericExactBounds<? extends @NonNull Object> wildcardOutside = new MyGenericExactBounds<>();
    MyGenericExactBounds<? extends @NonNull Number> wildcardInsideUB = wildcardOutside;

    MyGenericExactBounds<?> wildcardOutsideUB = wildcardOutside;
}
