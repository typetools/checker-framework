// An enhanced `for` loop whose body reassigns the loop variable before disposing of it does not
// dispose of the collection's elements.

import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class EnhancedForLoopVariableReassigned {

  // The loop closes `other` on every iteration, never an element of `resources`.
  // :: error: unfulfilled.collection.obligations
  void reassignThenDispose(@OwningCollection List<Resource> resources, Resource other) {
    for (Resource r : resources) {
      r = other;
      r.close();
      r.flush();
    }
  }

  // The loop disposes of every element, through the alias `t`.
  void aliasThenReassign(@OwningCollection List<Resource> resources, Resource other) {
    for (Resource r : resources) {
      Resource t = r;
      r = other;
      t.close();
      t.flush();
    }
  }

  // The loop disposes of every element: after `r = t`, `r` refers to the element again.
  void aliasThenReassignBack(@OwningCollection List<Resource> resources) {
    for (Resource r : resources) {
      Resource t = r;
      r = t;
      r.close();
      r.flush();
    }
  }

  // The loop disposes of every element: `r = r` does not change what `r` refers to.
  void selfAssign(@OwningCollection List<Resource> resources) {
    for (Resource r : resources) {
      r = r;
      r.close();
      r.flush();
    }
  }

  // The loop closes `other` through `r`; the alias `t` refers to the element but is never closed.
  // :: error: unfulfilled.collection.obligations
  void aliasThenReassignThenDispose(@OwningCollection List<Resource> resources, Resource other) {
    for (Resource r : resources) {
      Resource t = r;
      r = other;
      r.close();
      r.flush();
    }
  }
}
