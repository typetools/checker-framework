// An enhanced `for` loop whose body reassigns the loop variable before disposing of it does not
// dispose of the collection's elements.

import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class EnhancedForLoopVariableReassigned {

  // The loop closes `other` on every iteration, never an element of `resources`.
  // TODO: This is a false negative.  The checker should report unfulfilled.collection.obligations
  // on this method.  The loop-body analysis starts after the block that assigns the loop variable,
  // so it does not see the assignment `r = other`.
  void reassignThenDispose(@OwningCollection List<Resource> resources, Resource other) {
    for (Resource r : resources) {
      r = other;
      r.close();
      r.flush();
    }
  }

  // The loop disposes of every element, through the alias `t`.
  // TODO: This is a false positive.  The loop-body analysis does not see the assignment `t = r`,
  // so it does not know that `t` is the element.
  // :: error: unfulfilled.collection.obligations
  void aliasThenReassign(@OwningCollection List<Resource> resources, Resource other) {
    for (Resource r : resources) {
      Resource t = r;
      r = other;
      t.close();
      t.flush();
    }
  }
}
