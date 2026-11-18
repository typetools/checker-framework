// A `@PolyOwningCollection` return value never owns elements, and is bottom for non-resources.

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

class PolyOwningCollectionReturn {

  static void dispose(@OwningCollection Collection<Resource> c) {
    for (Resource r : c) {
      r.close();
      r.flush();
    }
  }

  static @PolyOwningCollection List<Resource> self(@PolyOwningCollection List<Resource> l) {
    return l;
  }

  static void takeIterator(Iterator<String> it) {}

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

  void iteratorRemoveWithoutObligation() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
      r.flush();
    }
    Iterator<Resource> it = l.iterator();
    it.next();
    // False positive: `l` has no obligation.
    // :: error: method.invocation
    it.remove();
  }

  void insertThroughAlias() {
    List<Resource> l = new ArrayList<>();
    List<Resource> alias = self(l);
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    // :: error: required.method.not.called
    alias.add(new Resource());
    l.clear();
  }

  void readThroughAlias() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    List<Resource> alias = self(l);
    alias.size();
    dispose(l);
  }

  void iterateOverAlias() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    for (Resource r : self(l)) {
      r.flush();
    }
    dispose(l);
  }

  void subListOfArrayList() {
    ArrayList<Resource> l = new ArrayList<>();
    l.add(new Resource());
    List<Resource> view = l.subList(0, 1);
    dispose(l);
  }

  void nonResourceIterator(List<String> l) {
    Iterator<String> it = l.iterator();
    it.next();
    it.remove();
    takeIterator(l.iterator());
  }
}
