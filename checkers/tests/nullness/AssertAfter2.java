import checkers.nullness.quals.*;

import java.util.*;

public class AssertAfter2 {

  public class Graph<T> {

    HashMap<T, List<@KeyFor("childMap") T>> childMap;

    @AssertNonNullAfter("childMap.get(#0)")
    public void addNode( T n ) {
      // body omitted, not relevant to test case
    }

    public void addEdge( T parent, T child ) {
      addNode(parent);
      @NonNull List<@KeyFor("childMap") T> l = childMap.get(parent);
    }

  }

}
