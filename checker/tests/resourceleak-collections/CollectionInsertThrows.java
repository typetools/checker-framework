import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.checkerframework.checker.collectionownership.qual.OwningCollection;

/*
 * Tests collections on exceptional exits. A collection insertion that throws does not take
 * ownership of the inserted element: BlockingQueue.put can throw InterruptedException before
 * inserting, so on that path the caller still owns the element and must dispose it. A collection
 * must also be disposed on exceptional exits, and a callee owns an @OwningCollection parameter only
 * if it returns normally.
 */
class CollectionInsertThrows {

  static void mayFail() throws IOException {}

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

  /*
   * A collection that has received an element leaks if a call throws before it is disposed.
   */
  void collectionLeaksOnExceptionalExit() throws IOException {
    List<Resource> list = new ArrayList<>();
    // :: error: (unfulfilled.collection.obligations)
    list.add(new Resource());
    mayFail();
    for (Resource r : list) {
      r.flush();
      r.close();
    }
  }

  /*
   * Disposing the collection in a finally block covers the exceptional exit: no error.
   */
  void collectionDisposedInFinally() throws IOException {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    try {
      mayFail();
    } finally {
      for (Resource r : list) {
        r.flush();
        r.close();
      }
    }
  }

  /*
   * The callee owns an @OwningCollection parameter only if it returns
   * normally, so it need not dispose the collection when it throws.
   */
  static void consumeOrThrow(@OwningCollection List<Resource> list) throws IOException {
    mayFail();
    for (Resource r : list) {
      r.flush();
      r.close();
    }
  }

  /*
   * If the call throws, the caller still owns the collection.
   */
  void callerOwnsCollectionWhenCallThrows() throws IOException {
    List<Resource> list = new ArrayList<>();
    // :: error: (unfulfilled.collection.obligations)
    list.add(new Resource());
    consumeOrThrow(list);
  }

  /*
   * The caller disposes the collection when the call throws: no error.
   */
  void callerDisposesCollectionWhenCallThrows() throws IOException {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    try {
      consumeOrThrow(list);
    } catch (IOException e) {
      for (Resource r : list) {
        r.flush();
        r.close();
      }
      throw e;
    }
  }
}
