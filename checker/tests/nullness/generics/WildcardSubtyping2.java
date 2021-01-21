import org.checkerframework.checker.nullness.qual.*;

public class WildcardSubtyping2 {
    class MyClass {}

    class MyCloneClass extends MyClass implements Cloneable {}

    class MyGeneric<@NonNull T extends @Nullable MyClass> {}

    class UseMyGeneric {
        MyGeneric<@NonNull MyCloneClass> nonNull = new MyGeneric<>();
        MyGeneric<@Nullable MyCloneClass> nullable = new MyGeneric<>();

        MyGeneric<? extends @NonNull Cloneable> interfaceNN = nonNull;
        MyGeneric<? extends @Nullable Cloneable> interfaceNull = nullable;
    }

    class MyGenericEB<@NonNull T extends @NonNull MyClass> {}

    class UseMyGenericEB {
        MyGenericEB<@NonNull MyCloneClass> nonNull = new MyGenericEB<>();
        // :: error: (type.argument.type.incompatible)
        MyGenericEB<@Nullable MyCloneClass> nullable = new MyGenericEB<>();

        MyGenericEB<? extends @NonNull Cloneable> interfaceNN = nonNull;
        MyGenericEB<? extends @Nullable Cloneable> interfaceNull = nullable;
    }
}
