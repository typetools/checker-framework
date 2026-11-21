// Reading an element from a collection or its view does not transfer ownership.

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

class BorrowedElementReads {

  // `Vector.get` and `Vector.firstElement` return borrowed resources.
  void getAndFirstElementDoNotTransferOwnership(Vector<Resource> c) {
    Resource r = c.get(0);
    Resource first = c.firstElement();
  }

  // `ArrayDeque.getFirst` and `ArrayDeque.peek` return borrowed resources.
  void getFirstAndPeekDoNotTransferOwnership(ArrayDeque<Resource> c) {
    Resource first = c.getFirst();
    Resource head = c.peek();
  }

  // `ListIterator.next` and `ListIterator.previous` return borrowed resources.
  void nextAndPreviousDoNotTransferOwnership(ListIterator<Resource> c) {
    Resource next = c.next();
    Resource previous = c.previous();
  }

  // `Collections.min` returns a borrowed resource.
  void minDoesNotTransferOwnership(Collection<Resource> c) {
    Resource r = Collections.min(c, (a, b) -> 0);
  }

  // Reading through a synchronized list returns a borrowed resource.
  void synchronizedListGetDoesNotTransferOwnership(List<Resource> c) {
    List<Resource> view = Collections.synchronizedList(c);
    Resource r = view.get(0);
  }
}
