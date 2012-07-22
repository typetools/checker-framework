import checkers.nullness.quals.*;
import java.io.*;
import java.util.*;

@checkers.quals.DefaultQualifier("Nullable")
public class GenericArgs {

    public @NonNull Set<@NonNull String> strings = new HashSet<@NonNull String>();

    void test() {
        @NonNull HashSet<@NonNull String> s = new HashSet<@NonNull String>();

        strings.addAll(s);
        strings.add("foo");
    }

    static class X<T extends @NonNull Object> {
        T value() {
            //:: error: (return.type.incompatible)
            return null;
        }
    }

    public static void test2() {
        //:: error: (type.argument.type.incompatible)
        Object o = new X<Object>().value();
    }

    static <Z extends @NonNull Object> void test3(Z z) {

    }

    void test4() {
        //:: error: (type.argument.type.incompatible)
        GenericArgs.<@Nullable Object>test3(null);
        //:: error: (argument.type.incompatible)
        GenericArgs.<@NonNull Object>test3(null);
    }

    static class GenericConstructor {
        <T extends @NonNull Object> GenericConstructor(T t) {

        }
    }

    void test5() {
        //:: error: (argument.type.incompatible)
        new <@NonNull String> GenericConstructor(null);
    }

    void testRecursiveDeclarations() {
        class MyComparator<T extends @NonNull Comparable<T>>
        implements Comparator<T @NonNull []> {
            public int compare(T[] a, T[] b) { return 0; }
        }
        Comparator<@NonNull String @NonNull []> temp = new MyComparator<@NonNull String>();
    }
}
