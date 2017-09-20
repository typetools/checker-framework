// Test case for Issue 415
// https://github.com/typetools/checker-framework/issues/415

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

public final class Issue415 {

    Map<String, Integer> mymap = new HashMap<String, Integer>();
    //:: error: (expression.unparsable.type.invalid)
    public static void usesField(Set<@KeyFor("this.mymap") String> keySet) {
        //:: error: (expression.unparsable.type.invalid) :: error: (argument.type.incompatible)
        new ArrayList<@KeyFor("this.mymap") String>(keySet);
    }

    public static void usesParameter(Map<String, Integer> m, Set<@KeyFor("#1") String> keySet) {
        new ArrayList<@KeyFor("#1") String>(keySet);
    }

    public static void sortedKeySet1(Map<String, Integer> m, Set<@KeyFor("#1") String> keySet) {
        new ArrayList<@KeyFor("#1") String>(keySet);
    }

    public static void sortedKeySet2(Map<String, Integer> m) {
        Set<@KeyFor("#1") String> keySet = m.keySet();
    }

    public static void sortedKeySet3(Map<String, Integer> m) {
        Set<@KeyFor("#1") String> keySet = m.keySet();
        new ArrayList<@KeyFor("#1") String>(keySet);
    }

    public static void sortedKeySet4(Map<String, Integer> m) {
        new ArrayList<@KeyFor("#1") String>(m.keySet());
    }

    public static <K extends Comparable<? super K>, V> void sortedKeySet(Map<K, V> m) {
        new ArrayList<@KeyFor("#1") K>(m.keySet());
    }
}
