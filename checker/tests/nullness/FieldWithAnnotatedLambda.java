import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

class FieldWithAnnotatedLambda {
    Function<Object, @Nullable Object> f1 = (@Nullable Object in) -> in;

    class Outer {
        class Inner {}
    }

    Function<Outer.Inner, @Nullable Object> f2 = (Outer.@Nullable Inner in) -> in;

    // Lambda parameter is inferred to also be @Nullable, raising an error
    // for the return expression here.
    // :: error: (return.type.incompatible)
    Function<Outer.@Nullable Inner, Object> f3 = (Outer.Inner in) -> in;
}
