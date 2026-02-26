import java.io.*;
import java.io.Closeable;
import java.util.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class LoopBodyAnalysisWhileTests {

  // -------------------------
  // WHILE LOOP: Iterator.hasNext/next
  // -------------------------
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

  // -------------------------
  // WHILE LOOP: Queue.isEmpty/poll
  // -------------------------
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

  // -------------------------
  // WHILE LOOP: Stack.isEmpty/pop (FULL)
  // -------------------------
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

  // -------------------------
  // NEGATIVE: Iterator while-loop missing flush
  // -------------------------

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

  // Helper used by the tests (same as your for-loop file).
  // :: error: illegal.type.annotation
  void checkArgIsOCWO(@OwningCollectionWithoutObligation Iterable<Resource> arg) {}

  // :: error: illegal.type.annotation
  void checkArgIsOCWO2(@OwningCollectionWithoutObligation Iterable<FileInputStream> arg) {}
}

abstract class RLCCollections {

  abstract Closeable alloc();

  void foo() throws Exception {
    @OwningCollection Collection<Closeable> resources = new ArrayList<Closeable>();
    resources.add(alloc());

    for (var r : resources) {
      try {
        r.close();
      } catch (IOException e) {
      }
    }
  }
}
