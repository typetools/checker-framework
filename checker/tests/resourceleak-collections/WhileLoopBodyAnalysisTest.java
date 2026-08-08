import java.io.*;
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
   * checked exceptions.
   */
  void whileIteratorPartialShouldError2(@OwningCollection List<FileInputStream> resources)
      throws IOException {
    Iterator<FileInputStream> it = resources.iterator();
    while (it.hasNext()) {
      FileInputStream r = it.next();
      try {
        r.close();
      } catch (IOException e) {
      }
    }
    checkArgIsOCWO2(resources);
  }

  // :: error: illegal.type.annotation
  void checkArgIsOCWO(@OwningCollectionWithoutObligation Iterable<Resource> arg) {}

  // :: error: illegal.type.annotation
  void checkArgIsOCWO2(@OwningCollectionWithoutObligation Iterable<FileInputStream> arg) {}
}
