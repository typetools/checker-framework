// A call that inserts into a @NotOwningCollection receiver is illegal even if an argument
// reassigns the receiver variable to an owning collection.  The call inserts into the collection
// that the receiver referred to before the reassignment.

import java.util.ArrayList;
import java.util.List;

class CollectionMutatorReceiverReassignedInArgument {

  void reassignReceiverInArgument(List<Resource> borrowed) {
    List<Resource> x = borrowed;
    List<Resource> owned = new ArrayList<>();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    x.add((x = owned).size(), new Resource());
    for (Resource r : x) {
      r.close();
      r.flush();
    }
  }
}
