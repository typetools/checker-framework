// An iterator over a collection of resources is @NotOwningCollection: it never owns the elements,
// so it cannot remove them.  An iterator over a collection of non-resources has no restrictions.

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

class CollectionIterators {

  static void dispose(@OwningCollection Collection<Resource> c) {
    for (Resource r : c) {
      r.close();
      r.flush();
    }
  }

  static void takeIterator(Iterator<String> it) {}

  void nonResourceIterator(List<String> l) {
    Iterator<String> it = l.iterator();
    it.next();
    it.remove();
    takeIterator(l.iterator());
  }

  void readThroughIterator() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    Iterator<Resource> it = l.iterator();
    while (it.hasNext()) {
      it.next().flush();
    }
    dispose(l);
  }

  void removeWithObligation() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    Iterator<Resource> it = l.iterator();
    it.next();
    // :: error: method.invocation
    it.remove();
    dispose(l);
  }

  void removeAfterCollectionGainsObligation() {
    // The iterator of a ConcurrentLinkedQueue sees elements inserted after its creation.
    Queue<Resource> q = new ConcurrentLinkedQueue<>();
    Iterator<Resource> it = q.iterator();
    q.add(new Resource());
    it.next();
    // :: error: method.invocation
    it.remove();
    dispose(q);
  }

  void removeWithoutObligation() {
    List<Resource> l = new ArrayList<>();
    Iterator<Resource> it = l.iterator();
    it.next();
    // False positive: `l` has no obligation.
    // :: error: method.invocation
    it.remove();
  }

  @OwningCollection
  ListIterator<Resource> returnIteratorAsOwning(List<Resource> l) {
    // :: error: return
    return l.listIterator();
  }

  // An override of Iterable.iterator() returns @NotOwningCollection, like the method it overrides.
  @InheritableMustCall("close")
  static class Pool implements Iterable<Resource> {
    final @OwningCollection List<Resource> rs = new ArrayList<>();

    @Override
    public Iterator<Resource> iterator() {
      return rs.iterator();
    }

    @CollectionFieldDestructor("this.rs")
    void close() {
      for (Resource r : rs) {
        r.close();
        r.flush();
      }
    }
  }

  void removeThroughPoolIterator(Pool p) {
    Iterator<Resource> it = p.iterator();
    it.next();
    // :: error: method.invocation
    it.remove();
  }
}
