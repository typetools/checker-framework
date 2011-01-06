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

    public void addEdgeBad1( T parent, T child ) {
    	//:: (assignment.type.incompatible)
        @NonNull List<@KeyFor("childMap") T> l = childMap.get(parent);
    }

    public void addEdgeBad2( T parent, T child ) {
        addNode(parent);
        //:: (assignment.type.incompatible)
        @NonNull List<@KeyFor("childMap") T> l = childMap.get(child);
    }

    public void addEdgeBad3( T parent, T child ) {
        addNode(parent);
        parent = child;
        //:: (assignment.type.incompatible)
        @NonNull List<@KeyFor("childMap") T> l = childMap.get(parent);
      }

    public void addEdgeOK( T parent, T child ) {
        List<@KeyFor("childMap") T> l = childMap.get(parent);
    }
  }

  class MultiParam {
	  MultiParam thing;
	  
	  // TODO: doc: spaces important
	  // TODO: doc: no explicit this!
	  @AssertNonNullAfter("get(#0, #1, #2)")
	  void add( Object o1, Object o2, Object o3 ) {
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
		  //:: (assignment.type.incompatible)
		  @NonNull Object nn = get(f1, f2, f3);
	  }
  }
}
