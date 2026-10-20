import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * Tests that a collection insertion that throws does not take ownership of the inserted element.
 * BlockingQueue.put can throw InterruptedException before inserting, so on that path the caller
 * still owns the element and must dispose it.
 */
class CollectionInsertThrows {

  /*
   * put() throws, and the element is not disposed on the exceptional path.
   */
  void elementLeaksWhenInsertThrows() {
    BlockingQueue<Resource> queue = new LinkedBlockingQueue<>();
    // :: error: (required.method.not.called)
    Resource r = new Resource();
    try {
      queue.put(r);
    } catch (InterruptedException e) {
      // r was not inserted, and it is not disposed on this path.
      return;
    }
    for (Resource x : queue) {
      x.flush();
      x.close();
    }
  }

  /*
   * put() throws, and the element is disposed on the exceptional path: no error.
   */
  void elementDisposedWhenInsertThrows() {
    BlockingQueue<Resource> queue = new LinkedBlockingQueue<>();
    Resource r = new Resource();
    try {
      queue.put(r);
    } catch (InterruptedException e) {
      r.flush();
      r.close();
      return;
    }
    for (Resource x : queue) {
      x.flush();
      x.close();
    }
  }
}
