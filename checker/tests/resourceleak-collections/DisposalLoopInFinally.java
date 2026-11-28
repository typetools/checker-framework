// A disposal loop in a `finally` block disposes of the collection on every path through the
// `try` block, including a `return`.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class DisposalLoopInFinally {

  void enhancedFor(@OwningCollection List<Resource> list, boolean b) {
    try {
      if (b) {
        return;
      }
      System.out.println();
    } finally {
      for (Resource r : list) {
        r.close();
        r.flush();
      }
    }
  }

  void indexedFor(@OwningCollection List<Resource> list, boolean b) {
    try {
      if (b) {
        return;
      }
      System.out.println();
    } finally {
      for (int i = 0; i < list.size(); i++) {
        list.get(i).close();
        list.get(i).flush();
      }
    }
  }

  void local(boolean b) {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    try {
      if (b) {
        return;
      }
      System.out.println();
    } finally {
      for (Resource r : list) {
        r.close();
        r.flush();
      }
    }
  }
}
