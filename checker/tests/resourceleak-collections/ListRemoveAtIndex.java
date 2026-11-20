// A borrowed reference may remove an element with `List.remove(int)`, as with `Queue.poll()`; the
// caller then owns the returned element.

import java.util.ArrayList;
import java.util.List;

class ListRemoveAtIndex {

  void removeFromBorrowed(List<Resource> borrowed) {
    Resource r = borrowed.remove(0);
    r.close();
    r.flush();
  }

  void discardFromBorrowed(List<Resource> borrowed) {
    // :: error: required.method.not.called
    borrowed.remove(0);
  }

  // Removing one element does not fulfill the collection's obligation.
  void removeFromOwning() {
    List<Resource> l = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    l.add(new Resource());
    Resource r = l.remove(0);
    r.close();
    r.flush();
  }
}
