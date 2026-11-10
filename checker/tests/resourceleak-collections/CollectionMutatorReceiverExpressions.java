// The receiver of a collection mutator can be any expression, not just a variable or field.

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class CollectionMutatorReceiverExpressions {

  // `r` is not owning, so inserting it into a non-owning collection is permitted.
  void conditionalReceiver(boolean b, List<Resource> l1, List<Resource> l2, Resource r) {
    (b ? l1 : l2).add(r);
  }

  void castReceiver(List<Resource> l, Resource r) {
    ((Collection<Resource>) l).add(r);
  }

  void assignmentReceiver(List<Resource> p, Resource r) {
    List<Resource> l;
    (l = p).add(r);
  }

  // The new list, and the resource in it, are never disposed of.
  void newReceiver() {
    // :: error: unfulfilled.collection.obligations
    new ArrayList<Resource>().add(new Resource());
  }
}
