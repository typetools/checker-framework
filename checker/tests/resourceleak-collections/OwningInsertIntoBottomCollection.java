// `Deque.push` takes ownership of its argument.  A collection that cannot hold an obligation
// cannot take ownership, so the pushed resource's obligation remains.

import java.util.Deque;
import java.util.List;

class OwningInsertIntoBottomCollection {

  void push(List<Deque<Resource>> deques) {
    for (Deque<Resource> d : deques) {
      // :: error: required.method.not.called
      d.push(new Resource());
    }
  }

  void add(List<Deque<Resource>> deques) {
    for (Deque<Resource> d : deques) {
      // :: error: required.method.not.called
      d.add(new Resource());
    }
  }
}
