// Synchronized and checked collection views borrow the elements of their backing collections.

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.Owning;

class CollectionWrapperViews {

  static void dispose(@OwningCollection Collection<Resource> c) {
    for (Resource r : c) {
      r.close();
      r.flush();
    }
  }

  // A synchronized set view cannot clear resources owned by the backing set.
  void clearThroughSynchronizedView() {
    Set<Resource> s = new HashSet<>();
    s.add(new Resource());
    // :: error: method.invocation
    Collections.synchronizedSet(s).clear();
    dispose(s);
  }

  // A synchronized set view cannot accept ownership of a resource.
  void insertThroughSynchronizedView() {
    Set<Resource> s = new HashSet<>();
    Set<Resource> view = Collections.synchronizedSet(s);
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    // :: error: required.method.not.called
    view.add(new Resource());
    dispose(s);
  }

  // A synchronized set view may read resources owned by the backing set.
  void readThroughSynchronizedView() {
    Set<Resource> s = new HashSet<>();
    s.add(new Resource());
    for (Resource r : Collections.synchronizedSet(s)) {
      r.flush();
    }
    dispose(s);
  }

  // A synchronized set of strings may insert and discard elements.
  void synchronizedViewOfStrings() {
    Set<String> s = Collections.synchronizedSet(new HashSet<>());
    s.add("a");
    s.clear();
  }

  // A checked list view cannot clear resources or accept ownership of an inserted resource.
  void checkedListViewCannotClearOrInsert(@OwningCollection List<Resource> c, @Owning Resource r) {
    List<Resource> view = Collections.checkedList(c, Resource.class);
    // :: error: method.invocation
    view.clear();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    view.add(r);
    r.flush();
    r.close();
    for (Resource e : c) {
      e.flush();
      e.close();
    }
  }
}
