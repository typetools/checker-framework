// A `@PolyOwningCollection` result, such as an iterator, refers to the same collection as the
// argument.  After the collection gains an obligation, the alias cannot discard elements.

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.checkerframework.checker.collectionownership.qual.*;

class PolyOwningCollectionAlias {

  static void dispose(@OwningCollection Collection<Resource> c) {
    for (Resource r : c) {
      r.close();
      r.flush();
    }
  }

  static @PolyOwningCollection List<Resource> self(@PolyOwningCollection List<Resource> l) {
    return l;
  }

  void iteratorRemoveAfterAdd() {
    // The iterator of a ConcurrentLinkedQueue sees elements inserted after its creation.
    Queue<Resource> q = new ConcurrentLinkedQueue<>();
    Iterator<Resource> it = q.iterator();
    q.add(new Resource());
    it.next();
    // :: error: method.invocation
    it.remove();
    dispose(q);
  }

  void clearAliasAfterAdd() {
    List<Resource> l = new ArrayList<>();
    List<Resource> alias = self(l);
    l.add(new Resource());
    // :: error: method.invocation
    alias.clear();
    dispose(l);
  }
}
