/*>>>
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;
*/

//https://code.google.com/p/checker-framework/issues/detail?id=415

import java.util.*;

public final class Issue415 {

    Map<String, Integer> mymap = new HashMap<String, Integer>();

    public static void usesField(Set</*@KeyFor("this.mymap")*/ String> keySet) {
        new ArrayList</*@KeyFor("this.mymap")*/ String>(keySet);
    }


    public static void usesParameter(Map<String, Integer> m, Set</*@KeyFor("#1")*/ String> keySet) {
        new ArrayList</*@KeyFor("#1")*/ String>(keySet);
    }


    public static void sortedKeySet1(Map<String, Integer> m, Set</*@KeyFor("#1")*/ String> keySet) {
        new ArrayList</*@KeyFor("#1")*/ String>(keySet);
    }


    public static void sortedKeySet2(Map<String, Integer> m) {
        Set</*@KeyFor("#1")*/ String> keySet = m.keySet();
    }


    public static void sortedKeySet3(Map<String, Integer> m) {
        Set</*@KeyFor("#1")*/ String> keySet = m.keySet();
        new ArrayList</*@KeyFor("#1")*/ String>(keySet);
    }


    public static void sortedKeySet4(Map<String, Integer> m) {
        new ArrayList</*@KeyFor("#1")*/ String>(m.keySet());
    }


    public static <K extends Comparable<? super K>, V> void sortedKeySet(Map<K, V> m) {
        new ArrayList</*@KeyFor("#1")*/ K>(m.keySet());
    }
}
