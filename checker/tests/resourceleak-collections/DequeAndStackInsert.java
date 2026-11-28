// Inserting into an owning `Deque` or `Stack` transfers the inserted element's obligation to the
// collection, as `Collection.add` does.

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;
import java.util.Vector;
import org.checkerframework.checker.collectionownership.qual.NotOwningCollection;

class DequeAndStackInsert {

  void dequeInsertsDisposed() {
    Deque<Resource> d = new ArrayDeque<>();
    d.push(new Resource());
    d.offer(new Resource());
    d.offerFirst(new Resource());
    d.offerLast(new Resource());
    d.addFirst(new Resource());
    d.addLast(new Resource());
    for (Resource r : d) {
      r.flush();
      r.close();
    }
  }

  void dequePushNotDisposed() {
    Deque<Resource> d = new ArrayDeque<>();
    // :: error: unfulfilled.collection.obligations
    d.push(new Resource());
  }

  void stackPushDisposed() {
    Stack<Resource> s = new Stack<>();
    s.push(new Resource());
    for (Resource r : s) {
      r.flush();
      r.close();
    }
  }

  void stackPushNotDisposed() {
    Stack<Resource> s = new Stack<>();
    // :: error: unfulfilled.collection.obligations
    s.push(new Resource());
  }

  void stackPushResultIsNotOwning() {
    Stack<Resource> s = new Stack<>();
    Resource pushed = s.push(new Resource());
    pushed.flush();
    for (Resource r : s) {
      r.flush();
      r.close();
    }
  }

  void pushIntoNotOwningDeque(@NotOwningCollection Deque<Resource> d) {
    Resource r = new Resource();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    d.push(r);
    r.flush();
    r.close();
  }

  // `Vector.addElement` transfers the resource obligation to the collection.
  void vectorAddElementDisposed() {
    Vector<Resource> c = new Vector<>();
    c.addElement(new Resource());
    for (Resource r : c) {
      r.flush();
      r.close();
    }
  }
}
