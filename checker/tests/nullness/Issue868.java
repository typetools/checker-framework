// Test case for Issue 868
// https://github.com/typetools/checker-framework/issues/868

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue868 {
    interface MyList {}

    <E extends @Nullable Object & @Nullable MyList> void test1(E e) {
        // :: error: (dereference.of.nullable)
        e.toString();
    }

    <E extends Object & @Nullable MyList> void test2(E e) {
        // :: error: (dereference.of.nullable)
        e.toString();
    }

    <E extends @Nullable Object & MyList> void test3(E e) {
        // :: error: (dereference.of.nullable)
        e.toString();
    }

    <E extends Object & MyList> void test4(E e) {
        e.toString();
    }
    // :: warning: (explicit.annotation.ignored)
    <E extends @NonNull Object & @Nullable MyList> void test5(E e) {
        e.toString();
    }

    // :: warning: (explicit.annotation.ignored)
    <E extends @Nullable Object & @NonNull MyList> void test6(E e) {
        // :: error: (dereference.of.nullable)
        e.toString();
    }

    void use() {
        this.<@Nullable MyList>test1(null);
        this.<@Nullable MyList>test2(null);
        this.<@Nullable MyList>test3(null);
        // :: error: (type.argument.type.incompatible)
        this.<@Nullable MyList>test4(null);
        // :: error: (type.argument.type.incompatible)
        this.<@Nullable MyList>test5(null);
        this.<@Nullable MyList>test6(null);
    }

    <T extends @Nullable Object & @Nullable MyList> void use2(T t, @NonNull T nonNullT) {
        this.<T>test1(t);
        // :: error: (argument.type.incompatible)
        this.<@NonNull T>test3(t);
        this.<@NonNull T>test3(nonNullT);
        // :: error: (type.argument.type.incompatible)
        this.<T>test5(t);
    }
}
