// `List.add(int, E)` and `List.set(int, E)` insert an element, just as `List.add(E)` does, so
// they create a collection obligation.

import java.util.ArrayList;
import java.util.List;

class ListInsertAtIndex {

  void addAtIndex() {
    List<Resource> l = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    l.add(0, new Resource());
  }

  void setAfterDisposal() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
      r.flush();
    }
    // :: error: unfulfilled.collection.obligations
    Resource old = l.set(0, new Resource());
    old.close();
    old.flush();
  }
}
