import org.checkerframework.checker.nullness.qual.*;

interface Consumer<A extends @Nullable Object> {
    public void consume(A object);
}

class Utils {

    public static <B extends @Nullable Object> Consumer<B> cast(
            final @Nullable Consumer<? super B> consumer) {
        throw new RuntimeException();
    }

    public static <C extends @Nullable Object> Consumer<C> getConsumer() {
        // null for simplicity, but could be anything
        Consumer<@Nullable Object> nullConsumer = null;

        // Previous reasoning for this to generate an (argument.type.incompatible) error was:
        // C could be @NonNull Object, so argument is incompatible?
        //
        // This is poor reasoning, however, because the type of the formal parameter should be:
        // @Nullable Consumer< ? [
        //                         super C[ extends @Nullable Object
        //                                  super @NonNull  Void
        //                                ]
        //                         extends @Nullable Object
        // ]
        // The primary annotations on nullConsumer and the formal parameter consumer are
        // identical, so it comes down to the annotations on the type arguments.

        // Let X stand in for the type argument of nullConsumer.  For it to be a valid
        // parameter, X must be contained by the type argument of the formal parameter,
        // ? super C.
        //
        // In other words, the following constraints must hold:
        //
        // C1: X <: upper bound of (? super C)
        // C2: lower bound of (? super C) <: X
        //
        // we can simplify these constraints by substituting out the lower and upper bound of
        // ? super C.
        // C1: X <: @Nullable Object
        // C2: C <: X
        //
        // we can simplify the constraints again by substituting X with the actual type argument to
        // nullConsumer and in C2, we can substitute C with its upper bound, since for the
        // constraint to hold X must be above C's upper bound.  This yields:
        //
        // C1: @Nullable Object <: @Nullable Object
        // C2: @Nullable Object <: @Nullable Object
        //
        // Since, for all type's T => T <: T, both C1 and C2 are upheld and the following statement
        // should NOT report an error
        Consumer<C> result = Utils.<C>cast(nullConsumer);

        // on a side note, I am not sure why this is called unexpected raw
        return result;
    }
}
