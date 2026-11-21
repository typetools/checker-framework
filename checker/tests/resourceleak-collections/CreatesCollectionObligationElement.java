// The parameter that a @CreatesCollectionObligation method inserts: by default the last one, or the
// one that `element` names.

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;

class CreatesCollectionObligationElement {

  static class Slots<E extends @MustCallUnknown Object> extends ArrayList<E> {
    @CreatesCollectionObligation(element = "#1")
    void insertAt(E e, int index) {
      add(index, e);
    }

    @CreatesCollectionObligation
    // :: error: creates.collection.obligation.element
    void insertAtLast(E e, int index) {
      add(index, e);
    }

    @CreatesCollectionObligation(element = "#3")
    // :: error: creates.collection.obligation.element.missing
    void insertAtMissing(E e, int index) {
      add(index, e);
    }
  }

  void insertAtNotDisposed() {
    Slots<Resource> slots = new Slots<Resource>();
    // :: error: unfulfilled.collection.obligations
    slots.insertAt(new Resource(), 0);
  }

  void insertAtDisposed() {
    Slots<Resource> slots = new Slots<Resource>();
    slots.insertAt(new Resource(), 0);
    for (Resource r : slots) {
      r.close();
      r.flush();
    }
  }

  void offerWithTimeout() throws InterruptedException {
    BlockingQueue<Resource> queue = new LinkedBlockingQueue<>();
    // If offer is interrupted, the resource is not inserted.
    // :: error: required.method.not.called
    // :: error: unfulfilled.collection.obligations
    queue.offer(new Resource(), 1, TimeUnit.SECONDS);
  }

  void putFirst() throws InterruptedException {
    BlockingDeque<Resource> deque = new LinkedBlockingDeque<>();
    // If putFirst is interrupted, the resource is not inserted.
    // :: error: required.method.not.called
    // :: error: unfulfilled.collection.obligations
    deque.putFirst(new Resource());
  }

  void insertElementAt() {
    Vector<Resource> vector = new Vector<>();
    // :: error: unfulfilled.collection.obligations
    vector.insertElementAt(new Resource(), 0);
  }
}
