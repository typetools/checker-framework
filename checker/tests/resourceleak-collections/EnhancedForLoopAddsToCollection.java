// An enhanced `for` loop whose body inserts into the collection does not dispose of the inserted
// elements.  For example, the iterator of a `CopyOnWriteArrayList` iterates over a snapshot, so the
// loop below exits normally, and the resources that it inserts are never closed.

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class EnhancedForLoopAddsToCollection {

  void addInLoop() {
    List<Resource> list = new CopyOnWriteArrayList<>();
    list.add(new Resource());
    for (Resource r : list) {
      r.close();
      r.flush();
      // :: error: unfulfilled.collection.obligations
      list.add(new Resource());
    }
  }
}
