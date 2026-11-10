// A `while` loop whose body inserts into the collection does not dispose of the inserted
// elements.  For example, the iterator of a `CopyOnWriteArrayList` iterates over a snapshot, so the
// loop below exits normally, and the resources that it inserts are never closed.

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class WhileLoopAddsToCollection {

  void addInIteratorLoop() {
    List<Resource> list = new CopyOnWriteArrayList<>();
    list.add(new Resource());
    Iterator<Resource> it = list.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
      // :: error: unfulfilled.collection.obligations
      list.add(new Resource());
    }
  }
}
