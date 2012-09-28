import checkers.nullness.quals.*;

import java.util.*;

public class AssertAfter2 {

  public class Graph<T> {

    HashMap<T, List<@KeyFor("childMap") T>> childMap;

    public Graph(HashMap<T, List<@KeyFor("childMap") T>> childMap) {
      this.childMap = childMap;
    }

    //:: warning: (nullness.parse.error)
    @AssertNonNullAfter("childMap.get(#0)") public void addNode( T n ) {
      // body omitted, not relevant to test case
    }

    public void addEdge( T parent, T child ) {
      addNode(parent);
      @NonNull List<@KeyFor("childMap") T> l = childMap.get(parent);
    }

    public void addEdgeBad1( T parent, T child ) {
      //:: error: (assignment.type.incompatible)
      @NonNull List<@KeyFor("childMap") T> l = childMap.get(parent);
    }

    public void addEdgeBad2( T parent, T child ) {
      addNode(parent);
      //:: error: (assignment.type.incompatible)
      @NonNull List<@KeyFor("childMap") T> l = childMap.get(child);
    }

    public void addEdgeBad3( T parent, T child ) {
      addNode(parent);
      parent = child;
      //:: error: (assignment.type.incompatible)
      @NonNull List<@KeyFor("childMap") T> l = childMap.get(parent);
    }

    public void addEdgeOK( T parent, T child ) {
      List<@KeyFor("childMap") T> l = childMap.get(parent);
    }
  }

  class MultiParam {

    MultiParam(MultiParam thing, Object f1, Object f2, Object f3) {
      this.thing = thing;
      this.f1 = f1;
      this.f2 = f2;
      this.f3 = f3;
    }

    MultiParam thing;

    // TODO: doc: spaces important
    // TODO: doc: no explicit this!
    //:: warning: (nullness.parse.error)
    @AssertNonNullAfter("get(#0, #1, #2)") void add( Object o1, Object o2, Object o3 ) {
      // body omitted, not relevant to test case
    }

    @Nullable Object get( Object o1, Object o2, Object o3 ) {
      return null;
    }

    Object f1, f2, f3;

    void addGood1() {
      thing.add(f1, f2, f3);
      @NonNull Object nn = thing.get(f1, f2, f3);
    }

    void addBad1() {
      //:: error: (assignment.type.incompatible)
      @NonNull Object nn = get(f1, f2, f3);
    }

    void addBad2() {
      thing.add(f1, f2, f3);
      f1 = new Object();
      //:: error: (assignment.type.incompatible)
      @NonNull Object nn = thing.get(f1, f2, f3);
    }

    void addBad3() {
      thing.add(f1, f2, f3);
      f2 = new Object();
      //:: error: (assignment.type.incompatible)
      @NonNull Object nn = thing.get(f1, f2, f3);
    }

    void addBad4() {
      thing.add(f1, f2, f3);
      f3 = new Object();
      //:: error: (assignment.type.incompatible)
      @NonNull Object nn = thing.get(f1, f2, f3);
    }
  }
}
