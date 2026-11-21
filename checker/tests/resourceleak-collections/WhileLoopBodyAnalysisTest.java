import java.io.*;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests certification of collection obligations for supported while-loop patterns.
 */
class WhileLoopBodyAnalysisTest {

  /*
   * Iterator.hasNext()/next() loops can certify collection obligations when every element is
   * fully satisfied.
   */
  void whileIteratorFull(@OwningCollection Collection<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
    }
    checkArgIsOCWO(resources);
  }

  void whileIteratorFullNullCheck(@OwningCollection Collection<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      if (r != null) {
        r.close();
        r.flush();
      }
    }
    checkArgIsOCWO(resources);
  }

  /*
   * Early exit prevents certification of the loop body, even if the loop closes the current
   * element before breaking.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorFullEarlyBreak(@OwningCollection Collection<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      if (resources.isEmpty()) {
        break;
      }
      r.flush();
    }
    // :: error: argument
    checkArgIsOCWO(resources);
  }

  /*
   * Queue.isEmpty()/poll() loops are also supported.
   */
  void whileQueuePollFull(@OwningCollection Queue<Resource> resources) {
    while (!resources.isEmpty()) {
      Resource r = resources.poll();
      if (r != null) {
        r.close();
        r.flush();
      }
    }
    checkArgIsOCWO(resources);
  }

  /*
   * Stack loops should work for both isEmpty()/pop() and size()/pop().
   */
  void whileStackPopFull(@OwningCollection Stack<Resource> resources) {
    while (!resources.isEmpty()) {
      Resource r = resources.pop();
      r.close();
      r.flush();
    }
    checkArgIsOCWO(resources);
  }

  void whileStackPopFullWithSize(@OwningCollection Stack<Resource> resources) {
    while (resources.size() > 0) {
      Resource r = resources.pop();
      r.close();
      r.flush();
    }
    checkArgIsOCWO(resources);
  }

  /*
   * Missing one of the required methods should prevent loop certification.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorPartialShouldError(@OwningCollection List<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      // missing flush
    }
    // :: error: argument
    checkArgIsOCWO(resources);
  }

  /*
   * The same partial-satisfaction case should fail for close-only resources that throw
   * checked exceptions: an element is not closed on some path.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorPartialShouldError2(
      @OwningCollection List<FileInputStream> resources, boolean b) throws IOException {
    Iterator<FileInputStream> it = resources.iterator();
    while (it.hasNext()) {
      FileInputStream r = it.next();
      if (b) {
        try {
          r.close();
        } catch (IOException e) {
        }
      }
    }
    // :: error: argument
    checkArgIsOCWO2(resources);
  }

  /*
   * Iterating over a view such as subList does not dispose of all the elements of the list.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorOverSubList(@OwningCollection List<Resource> resources) {
    Iterator<Resource> it = resources.subList(0, 1).iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
    }
  }

  /*
   * An iterator might not visit an element inserted during the iteration (for example, if the
   * list is a CopyOnWriteArrayList), so this loop is not a disposal loop.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorInsertsIntoCollection(@OwningCollection List<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
      resources.add(new Resource());
    }
  }

  /*
   * An extraction passed as an argument still advances the iterator, so this loop takes two
   * elements per iteration and disposes only one of them.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorExtractionAsArgument(@OwningCollection Collection<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      log(it.next());
      Resource r = it.next();
      r.close();
      r.flush();
    }
  }

  /*
   * An element taken from the iterator before the loop is not disposed by the loop.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorAdvancedBeforeLoop(@OwningCollection Collection<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    Resource first = it.next();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
    }
  }

  /*
   * An extraction in a loop nested in the body can take several elements per iteration of the
   * outer loop, and this loop disposes only the last of them.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorExtractionInNestedLoop(
      @OwningCollection Collection<Resource> resources, int n) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      Resource r = null;
      for (int i = 0; i < n; i++) {
        r = it.next();
      }
      if (r != null) {
        r.close();
        r.flush();
      }
    }
  }

  /*
   * forEachRemaining advances the iterator past elements that the loop does not dispose of.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorForEachRemaining(@OwningCollection Collection<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
      it.forEachRemaining(x -> {});
    }
  }

  /*
   * A method that receives the iterator might advance it.
   */
  // :: error: unfulfilled.collection.obligations
  void whileIteratorPassedToMethod(@OwningCollection Collection<Resource> resources) {
    Iterator<Resource> it = resources.iterator();
    while (it.hasNext()) {
      Resource r = it.next();
      r.close();
      r.flush();
      skip(it);
    }
  }

  /*
   * clear() discards elements that the loop does not dispose of.
   */
  // :: error: unfulfilled.collection.obligations
  void whileQueuePollThenClear(@OwningCollection Queue<Resource> resources) {
    while (!resources.isEmpty()) {
      Resource r = resources.poll();
      if (r != null) {
        r.close();
        r.flush();
      }
      resources.clear();
    }
  }

  void log(Resource r) {}

  void skip(Iterator<Resource> it) {}

  // Two calls to newQueue() can return different queues, so this loop is not a disposal loop. The
  // checker used to crash on it.
  void whileOverUnqualifiedCall() {
    // :: error: unfulfilled.collection.obligations
    while (!newQueue().isEmpty()) {
      // :: error: unfulfilled.collection.obligations
      Resource r = newQueue().poll();
      if (r != null) {
        r.close();
        r.flush();
      }
    }
  }

  @OwningCollection
  Queue<Resource> newQueue() {
    Queue<Resource> queue = new ArrayDeque<>();
    queue.add(new Resource());
    return queue;
  }

  // :: error: illegal.type.annotation
  void checkArgIsOCWO(@OwningCollectionWithoutObligation Iterable<Resource> arg) {}

  // :: error: illegal.type.annotation
  void checkArgIsOCWO2(@OwningCollectionWithoutObligation Iterable<FileInputStream> arg) {}
}
