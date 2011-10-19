import checkers.nullness.quals.*;

import java.util.*;

public class KeyFors {

    public void withoutKeyFor() {
        Map<String, String> map = new HashMap<String, String>();
        String key = "key";

        //:: error: (assignment.type.incompatible)
        @NonNull String value = map.get(key);
    }

    public void withKeyFor() {
        Map<String, String> map = new HashMap<String, String>();
        @SuppressWarnings("assignment.type.incompatible")
        @KeyFor("map") String key = "key";

        @NonNull String value = map.get(key);
    }

    public void withCollection() {
        Map<String, String> map = new HashMap<String, String>();
        List<@KeyFor("map") String> keys = new ArrayList<@KeyFor("map") String>();

        @KeyFor("map") String key = keys.get(0);
        @NonNull String value = map.get(key);
        // TODO when using the local variable the access works
        // when using the call directly, we get an assignment.type.incompatible.
        // Why?
        // TODO: value = map.get(keys.get(0));
    }

    public void withIndirectReference() {
        class Container {
            Map<String, String> map = new HashMap<String, String>();
        }

        Container container = new Container();
        @SuppressWarnings("assignment.type.incompatible")
        @KeyFor("container.map") String key = "m";

        @NonNull String value = container.map.get(key);
    }

    // Should this be '@KeyFor("#0")', or '@KeyFor("m")'?
    public static
    <K extends Comparable<? super K>,V> Collection<@KeyFor("#0") K>
    sortedKeySet(Map<K,V> m) {
        throw new RuntimeException();
    }

    public void testForLoop(HashMap<String, String> lastMap) {
        // TODO: support Flow for KeyFor
        Collection<@KeyFor("lastMap") String> sorted = sortedKeySet(lastMap);
        for (@KeyFor("lastMap") String key : sorted) {
            @NonNull String al = lastMap.get(key);
        }
    }
    
    static class Otherclass {
        static Map<String, String> map = new HashMap<String, String>();
    }

    public void testStaticKeyFor(@KeyFor("Otherclass.map") String s1, String s2) {
        Otherclass.map.get(s1).toString();
        //:: error: (dereference.of.nullable)
        Otherclass.map.get(s2).toString();

        Otherclass o = new Otherclass();
        o.map.get(s1).toString();
        //:: error: (dereference.of.nullable)
        o.map.get(s2).toString();        
    }

  public class Graph<T> {

    HashMap<T, List<@KeyFor("childMap") T>> childMap;

    public Graph(HashMap<T, List<@KeyFor("childMap") T>> childMap) {
        this.childMap = childMap;
    }

    public void addNode( T n ) {
      // body omitted, not relevant to test case
    }

    public void addEdge2( T parent, T child ) {
      addNode(parent);
      @SuppressWarnings("cast.unsafe")
      @KeyFor("childMap") T parent2 = (@KeyFor("childMap") T) parent;
      @NonNull List<@KeyFor("childMap") T> l = childMap.get(parent2);
    }

    // TODO: This is a feature request to have KeyFor inferred
//    public void addEdge3( T parent, T child ) {
//      addNode(parent);
//      parent = (@KeyFor("childMap") T) parent;
//      @NonNull List<T> l = childMap.get(parent);
//    }

  }

  /* TODO: add logic that after a call to "put" the first argument is
      annotated with @KeyFor. A "@KeyForAfter" annotation to
      support this in a general way might be overkill.
      Similarly, for calls to "remove" we need to invalidate all (?)
      KeyFor annotations.

    void keyForFlow() {
        Map<String, String> leaders = new LinkedHashMap<String, String>();
        Set<@KeyFor("leaders") String> varsUsedPreviously = new LinkedHashSet<@KeyFor("leaders") String>();
        String varName = "hello";
        leaders.put(varName, "goodbye");
        // TODO: add @KeyFor("leaders") to varName after put
        @KeyFor("leaders") String kf = varName;
    }
  */
}
