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
}
