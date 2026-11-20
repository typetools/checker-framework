// A `while` loop whose body inserts into the collection does not dispose of the inserted
// elements.  For example, the iterator of a `CopyOnWriteArrayList` iterates over a snapshot, so the
// loop below exits normally, and the resources that it inserts are never closed.

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

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

  @InheritableMustCall("close")
  static class Refill {
    @OwningCollection List<Resource> resources = new ArrayList<>();

    @CreatesMustCallFor("this")
    void addOne() {
      resources.add(new Resource());
    }

    // The loop body inserts into the field through a helper, so the loop does not dispose of the
    // inserted elements.
    @CreatesMustCallFor("this")
    void disposeAndRefill() {
      Iterator<Resource> it = resources.iterator();
      while (it.hasNext()) {
        Resource r = it.next();
        r.close();
        r.flush();
        addOne();
      }
      // :: error: unfulfilled.collection.obligations
      resources = new ArrayList<>();
    }

    @CollectionFieldDestructor("resources")
    void close() {
      Iterator<Resource> it = resources.iterator();
      while (it.hasNext()) {
        Resource r = it.next();
        r.close();
        r.flush();
      }
    }
  }
}
