// An indexed `for` loop whose body removes elements does not visit every element of the
// collection, so it does not dispose of all of them.

import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class IndexedLoopCollectionMutated {

  // Removing element i shifts the later elements down, so the loop skips every other element.  For
  // example, if `resources` is [a, b, c], then the loop disposes of a (twice) and c, but not b.
  // :: error: unfulfilled.collection.obligations
  void removeInLoop(@OwningCollection List<Resource> resources) {
    for (int i = 0; i < resources.size(); i++) {
      Resource r = resources.get(i);
      r.close();
      r.flush();
      Resource removed = resources.remove(i);
      removed.close();
      removed.flush();
    }
  }
}
