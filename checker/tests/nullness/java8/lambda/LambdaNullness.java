// Test file for nullness parameter and return checks.

import org.checkerframework.checker.nullness.qual.*;

interface Noop {
    void noop();
}

interface FunctionNull<T extends @Nullable Object, R> {
    R apply(T t);
}

interface Supplier<R extends @Nullable Object> {
    R supply();
}

interface BiFunctionNull<T, U, R> {
    R apply(T t, U u);
}

public class LambdaNullness {

    // Annotations in lamba expressions, in static, instance of fields initializers are stored on
    // the last declared constructor.
    //
    // For example, the annotation for @Nullable Integer x on f7's initializer
    // is stored on here because it is the last defined constructor.
    //
    // See TypeFromElement::annotateParam
    LambdaNullness(FunctionNull<String, String> f, Object e) {}

    // No parameters; result is void
    Noop f1 = () -> {};
    // No parameters, expression body
    Supplier<Integer> f2a = () -> 42;

    // No parameters, expression body
    // :: error: (return.type.incompatible)
    Supplier<Integer> f2b = () -> null;

    // No parameters, expression body
    Supplier<@Nullable Void> f3 = () -> null;
    // No parameters, block body with return
    Supplier<Integer> f4a =
            () -> {
                return 42;
            };
    // No parameters, block body with return
    Supplier<@Nullable Integer> f4b =
            () -> {
                // :: error: (assignment.type.incompatible)
                @NonNull String s = null;

                return null;
            };
    // No parameters, void block body
    Noop f5 =
            () -> {
                System.gc();
            };

    // Complex block body with returns
    Supplier<Integer> f6 =
            () -> {
                if (true) {
                    return 12;
                } else {
                    int result = 15;
                    for (int i = 1; i < 10; i++) {
                        result *= i;
                    }
                    // :: error: (return.type.incompatible)
                    return null;
                }
            };

    // Single declared-type parameter
    FunctionNull<@Nullable Integer, Integer> f7 = (@Nullable Integer x) -> 1;

    // Single declared-type parameter
    FunctionNull<@Nullable String, String> f9 =
            // :: error: (lambda.param.type.incompatible)
            (@NonNull String x) -> {
                return x + "";
            };
    // Single inferred-type parameter
    FunctionNull<@NonNull Integer, Integer> f10 = (x) -> x + 1;
    // Parentheses optional for single
    FunctionNull<@Nullable Integer, Integer> f11 = x -> 1;

    // Multiple declared-type parameters
    BiFunctionNull<Integer, Integer, Integer> f16 =
            (@Nullable Integer x, final Integer y) -> {
                x = null;
                // :: error: (unboxing.of.nullable)
                return x + y;
            };

    // Multiple inferred-type parameters
    BiFunctionNull<String, String, String> f18 = (x, y) -> x + y;

    // Infer based on context.
    FunctionNull<@Nullable String, String> fn =
            (s) -> {
                // :: error: (dereference.of.nullable)
                s.toString();
                return "";
            };
}
