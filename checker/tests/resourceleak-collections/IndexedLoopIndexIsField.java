// An indexed `for` loop is a disposal loop only if it accesses `collection.get(i)`, where `i` is
// the loop variable.  A field with the same name as the loop variable is a different variable.

import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class IndexedLoopIndexIsField {

  static class Holder {
    int i;
  }

  int i;

  // :: error: unfulfilled.collection.obligations
  void otherObjectsField(@OwningCollection List<Resource> list, Holder h) {
    for (int i = 0; i < list.size(); i++) {
      list.get(h.i).close();
      list.get(h.i).flush();
    }
  }

  // :: error: unfulfilled.collection.obligations
  void thisField(@OwningCollection List<Resource> list) {
    for (int i = 0; i < list.size(); i++) {
      list.get(this.i).close();
      list.get(this.i).flush();
    }
  }
}
