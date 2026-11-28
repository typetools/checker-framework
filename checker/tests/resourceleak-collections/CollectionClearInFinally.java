import java.util.*;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests that clearing an owning collection does not interfere with recognizing that each
 * element obligation was already discharged.
 */
class CollectionClearInFinally {
  /*
   * Check that a clear in the finally block is accepted after the loop closes and flushes
   * every element.
   */
  void clearInFinally(@OwningCollection List<Resource> resources) {
    try {
      for (Resource r : resources) {
        r.flush();
        r.close();
      }
    } finally {
      resources.clear();
    }
  }

  /*
   * Check the same scenario when the collection is cleared after the try/finally statement.
   */
  void clearAfterFinally(@OwningCollection List<Resource> resources) {
    try {
      for (Resource r : resources) {
        r.flush();
        r.close();
      }
    } finally {
    }
    resources.clear();
  }
}
