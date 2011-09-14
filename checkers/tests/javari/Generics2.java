import java.util.*;

/*
 * Test case for Issue 111
 * http://code.google.com/p/checker-framework/issues/detail?id=111
 */
public class Generics2 {
    /* This simple test makes sure that the same defaulting for type variable
     * upper bounds is used for parameters and the return type. */
    <K, V> Map<K, List<V>> test1(HashMap<K, List<V>> map, V val) {
        return map;
    }

    void m1(Map<String, List<int[]>> map) {
        // OK
        m2s(map, new int[0]);
        
        // This used to crash at
        // checkers.util.GraphQualifierHierarchy.checkAnnoInGraph(GraphQualifierHierarchy.java:244)
        // Primitive types were not annotated in all cases.
        m2(map, new int[0]);
    }

    <K, V> Map<K, List<V>> m2(Map<K, List<V>> map, V val) {
        return map;
    }

    <V> Map<String, List<int[]>> m2s(Map<String, List<int[]>> map, V val) {
        return map;
    }
    
    Map<String, List<int[]>> m3(Map<String, List<int[]>> map) {
        return map;
    }
    
    Map<String, List<int[]>> m3b() {
        return new HashMap<String, List<int[]>>();
    }
}