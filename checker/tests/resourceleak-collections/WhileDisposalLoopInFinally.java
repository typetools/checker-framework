// A `while` disposal loop in a `finally` block disposes of the collection on every path through
// the `try` block, including a `return`.

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import org.checkerframework.checker.collectionownership.qual.*;

class WhileDisposalLoopInFinally {

  void iteratorLoop(@OwningCollection List<Resource> list, boolean b) {
    try {
      if (b) {
        return;
      }
      System.out.println();
    } finally {
      Iterator<Resource> it = list.iterator();
      while (it.hasNext()) {
        Resource r = it.next();
        r.close();
        r.flush();
      }
    }
  }

  void drainLoop(@OwningCollection Queue<Resource> queue, boolean b) {
    try {
      if (b) {
        return;
      }
      System.out.println();
    } finally {
      while (!queue.isEmpty()) {
        Resource r = queue.poll();
        if (r != null) {
          r.close();
          r.flush();
        }
      }
    }
  }
}
