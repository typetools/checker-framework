import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.*;

public class KeyFors {

  public void withoutKeyFor() {
    Map<String, String> map = new HashMap<>();
    String key = "key";

    // :: error: (assignment)
    @NonNull String value = map.get(key);
  }

  public void withKeyFor() {
    Map<String, String> map = new HashMap<>();
    @SuppressWarnings("assignment")
    @KeyFor("map") String key = "key";

    @NonNull String value = map.get(key);
  }

  public void withCollection() {
    Map<String, String> map = new HashMap<>();
    List<@KeyFor("map") String> keys = new ArrayList<>();

    @KeyFor("map") String key = keys.get(0);
    @NonNull String value = map.get(key);
    value = map.get(keys.get(0));
  }

  public void withIndirectReference() {
    class Container {
      Map<String, String> map = new HashMap<>();
    }

    Container container = new Container();
    @SuppressWarnings("assignment")
    @KeyFor("container.map") String key = "m";

    @NonNull String value = container.map.get(key);
  }

  /** Returns a sorted version of m.keySet(). */
  public static <K extends Comparable<? super K>, V> Collection<@KeyFor("#1") K> sortedKeySet(
      Map<K, V> m) {
    throw new RuntimeException();
  }

  static HashMap<Integer, Object> call_hashmap = new HashMap<>();

  public void testForLoop(HashMap<String, String> lastMap) {
    Collection<@KeyFor("lastMap") String> sorted = sortedKeySet(lastMap);
    for (@KeyFor("lastMap") String key : sorted) {
      @NonNull String al = lastMap.get(key);
    }
    for (@KeyFor("call_hashmap") Integer i : sortedKeySet(call_hashmap)) {}
  }

  static class Otherclass {
    static Map<String, String> map = new HashMap<>();
  }

  public void testStaticKeyFor(@KeyFor("Otherclass.map") String s1, String s2) {
    Otherclass.map.get(s1).toString();
    // :: error: (dereference.of.nullable)
    Otherclass.map.get(s2).toString();

    Otherclass o = new Otherclass();
    o.map.get(s1).toString();
    // TODO:: error: (dereference.of.nullable)
    o.map.get(s2).toString();
  }

  public class Graph<T> {

    HashMap<T, List<@KeyFor("childMap") T>> childMap;

    public Graph(HashMap<T, List<@KeyFor("childMap") T>> childMap) {
      this.childMap = childMap;
    }

    public void addNode(T n) {
      // body omitted, not relevant to test case
    }

    public void addEdge2(T parent, T child) {
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
  KeyFor annotations.*/

  void keyForFlow() {
    Map<String, String> leaders = new LinkedHashMap<>();
    Set<@KeyFor("leaders") String> varsUsedPreviously =
        new LinkedHashSet<@KeyFor("leaders") String>();
    String varName = "hello";
    leaders.put(varName, "goodbye");
    @KeyFor("leaders") String kf = varName;
  }

  public static void mapPut(String start) {
    Map<String, Integer> n2e = new HashMap<>();
    n2e.put(start, Integer.valueOf(0));
    @KeyFor("n2e") String start2 = start;
  }
}
