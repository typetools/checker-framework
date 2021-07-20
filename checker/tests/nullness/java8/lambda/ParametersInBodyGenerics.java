// Test that parameter annotations are correct in the body of a lambda

import org.checkerframework.checker.nullness.qual.*;

import java.util.List;

public class ParametersInBodyGenerics {
    interface NullableConsumer {
        void method(List<@Nullable String> s);
    }

    interface NonNullConsumer {
        void method(@NonNull List<String> s);
    }

    void test() {
        // :: error: (lambda.param.type.incompatible)
        NullableConsumer fn0 = (List<String> i) -> i.get(0).toString();
        NullableConsumer fn2 =
                (List<@Nullable String> i) -> {
                    // :: error: (dereference.of.nullable)
                    i.get(0).toString();
                };
        NullableConsumer fn3 =
                // :: error: (lambda.param.type.incompatible)
                (List<String> i) -> {
                    i.get(0).toString();
                };
        NullableConsumer fn3b =
                (i) -> {
                    // :: error: (dereference.of.nullable)
                    i.get(0).toString();
                };

        NonNullConsumer fn4 =
                (List<String> i) -> {
                    i.get(0).toString();
                };
        NonNullConsumer fn4b =
                (i) -> {
                    i.get(0).toString();
                };
    }
}
