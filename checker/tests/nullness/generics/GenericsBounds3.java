import org.checkerframework.checker.nullness.qual.*;

public class GenericsBounds3 {
    class Sup<X extends @NonNull Object> {}

    // :: error: (type.argument.type.incompatible)
    class Sub extends Sup<@Nullable Object> {}

    class SubGood extends Sup<@NonNull Object> {}

    interface ISup<X extends @NonNull Object> {}

    // :: error: (type.argument.type.incompatible)
    class ISub implements ISup<@Nullable Object> {}

    class ISubGood implements ISup<@NonNull Object> {}

    // :: error: (type.argument.type.incompatible)
    class ISub2 extends Sup<Object> implements java.io.Serializable, ISup<@Nullable Object> {}
}
