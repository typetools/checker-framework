// Shapes of `while` disposal loops: parenthesized receivers and `continue` statements.

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import org.checkerframework.checker.collectionownership.qual.*;

class WhileDisposalLoopShapes {

  void parenthesizedExtractionReceiver(@OwningCollection Queue<Resource> q) {
    while (!q.isEmpty()) {
      Resource r = (q).remove();
      r.close();
      r.flush();
    }
  }

  void parenthesizedIteratorReceiver(@OwningCollection List<Resource> list) {
    Iterator<Resource> it = list.iterator();
    while (it.hasNext()) {
      Resource r = (it).next();
      r.close();
      r.flush();
    }
  }

  // A `continue` before the extraction gives the loop a second back edge.
  void continueBeforeExtraction(@OwningCollection Queue<Resource> q, boolean skip) {
    while (!q.isEmpty()) {
      if (skip) {
        skip = false;
        continue;
      }
      Resource r = q.remove();
      r.close();
      r.flush();
    }
  }
}
