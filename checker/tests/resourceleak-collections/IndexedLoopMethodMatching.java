// An indexed `for` loop is a disposal loop if it calls `List.get(int)` and `Collection.size()`, or
// methods that override them, such as `ArrayList.get(int)` and `ArrayList.size()`.

import java.util.ArrayList;
import org.checkerframework.checker.collectionownership.qual.*;

class IndexedLoopMethodMatching {

  void indexForLoopOverArrayList(@OwningCollection ArrayList<Resource> resources) {
    for (int i = 0; i < resources.size(); i++) {
      Resource r = resources.get(i);
      r.close();
      r.flush();
    }
  }

  // :: error: unfulfilled.collection.obligations
  void indexForLoopOverArrayListPartial(@OwningCollection ArrayList<Resource> resources) {
    for (int i = 0; i < resources.size(); i++) {
      Resource r = resources.get(i);
      r.close();
      // missing flush
    }
  }
}
