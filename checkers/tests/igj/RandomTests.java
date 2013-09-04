import java.util.*;
import checkers.igj.quals.*;

public class RandomTests {
    MyClass m = null;
    @Mutable Date d = m.get();

    static int i = 0;
    boolean[] array1 = { };
    static boolean[] array2 = { };
    void change() {
        i = 4;
        array1[2] = true;
        array2[3] = true;
    }

    class MyClass<T extends Date> { T get() { return null; } }

    void mytest() {
        MyClass<@ReadOnly Date> d1 = null;
        MyClass<@Mutable Date> d2 = null;

    }

    // Supertypes of anonymous classes are properly annotated
    void run(Date d) { }
    void test() {
        run(new Date() { });
    }

    // Random tests with wildcards
    public static <T> List<T> compound(Comparator<T> first) {
        return compound(java.util.Arrays.asList(first));
    }

    static <T> List<T> compound(Iterable<? extends Comparator<? super T>> comparators) { return null; }

    // test equality
    void testInference() {
        boolean b = null == Collections.emptyList().iterator();
    }

    // test capture
    public void filter() {
        Iterator<?> unfiltered = null;
        filter(unfiltered);
    }

    public static <T> Iterator<T> filter(Iterator<T> unfiltered) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> filter(Iterator<?> unfiltered, Class<T> type)
    {
      List<Object> predicate = null;
      return (Iterator<T>) filter(unfiltered, predicate);
    }

    public static <T> @I Iterator<T> filter(Iterator<T> unfiltered, List<? super T> predicate) {
        return null;
    }

    void testIntersection() {
        java.util.Arrays.asList("m", 1);
    }
}
