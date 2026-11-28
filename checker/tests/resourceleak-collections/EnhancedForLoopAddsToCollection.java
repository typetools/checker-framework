// An enhanced `for` loop whose body inserts into the collection does not dispose of the inserted
// elements.  For example, the iterator of a `CopyOnWriteArrayList` iterates over a snapshot, so the
// loop below exits normally, and the resources that it inserts are never closed.

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class EnhancedForLoopAddsToCollection {

  void addInLoop() {
    List<Resource> list = new CopyOnWriteArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Resource());
    for (Resource r : list) {
      r.close();
      r.flush();
      // :: error: unfulfilled.collection.obligations
      list.add(new Resource());
    }
  }

  void addInLoopThroughParenthesizedReceiver() {
    List<Resource> list = new CopyOnWriteArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Resource());
    for (Resource r : list) {
      r.close();
      r.flush();
      // :: error: unfulfilled.collection.obligations
      (list).add(new Resource());
    }
  }

  void addInLoopOverParenthesizedCollection() {
    List<Resource> list = new CopyOnWriteArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Resource());
    for (Resource r : (list)) {
      r.close();
      r.flush();
      // :: error: unfulfilled.collection.obligations
      ((list)).add(new Resource());
    }
  }

  @InheritableMustCall("close")
  static class Refill {
    @OwningCollection List<Resource> resources = new ArrayList<>();

    @CreatesMustCallFor("this")
    void addOne() {
      resources.add(new Resource());
    }

    @CreatesMustCallFor("this")
    void disposeAndRefill() {
      for (Resource r : resources) {
        r.close();
        r.flush();
        addOne();
      }
      // :: error: unfulfilled.collection.obligations
      resources = new ArrayList<>();
    }

    @CollectionFieldDestructor("resources")
    void close() {
      for (Resource r : resources) {
        r.close();
        r.flush();
      }
    }
  }
}
