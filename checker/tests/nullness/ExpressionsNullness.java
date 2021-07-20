import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.*;

@DefaultQualifier(NonNull.class)
public class ExpressionsNullness {

    public static double[] returnDoubleArray() {
        return new double[] {3.14, 2.7};
    }

    public static void staticMembers() {
        Pattern.compile("^>entry *()");
        System.out.println(ExpressionsNullness.class);
        ExpressionsNullness.class.getAnnotations(); // valid
    }

    private HashMap<String, String> map = new HashMap<>();

    public void test() {
        @SuppressWarnings("nullness")
        String s = map.get("foo");

        Class<?> cl = Boolean.TYPE;

        List<?> foo = new LinkedList<Object>();
        // :: error: (dereference.of.nullable)
        foo.get(0).toString(); // default applies to wildcard extends

        Set set = new HashSet();
        for (@Nullable Object o : set) System.out.println();
    }

    void test2() {
        List<? extends @NonNull String> lst = new LinkedList<@NonNull String>();
        for (String s : lst) {
            s.length();
        }
    }

    <T extends @NonNull Object> void test3(T o) {
        o.getClass(); // valid
    }

    void test4(List<? extends @NonNull Object> o) {
        o.get(0).getClass(); // valid
    }

    void test5() {
        Comparable<Date> d = new Date();
    }

    void testIntersection() {
        java.util.Arrays.asList("m", 1);
    }

    Object obj;

    public ExpressionsNullness(Object obj) {
        this.obj = obj;
    }
}
