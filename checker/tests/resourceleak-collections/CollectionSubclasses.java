// A subclass of a JDK collection class is a collection, just like the JDK class.  This includes a
// user-defined class and a nested JDK class such as `ConcurrentHashMap.KeySetView`.

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.collectionownership.qual.*;

class CollectionSubclasses {

  static class ResourceList<E> extends ArrayList<E> {}

  void leakArrayList() {
    List<Resource> list = new ArrayList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Resource());
  }

  void disposeArrayList() {
    List<Resource> list = new ArrayList<>();
    list.add(new Resource());
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }

  void leakUserDefined() {
    ResourceList<Resource> list = new ResourceList<>();
    // :: error: unfulfilled.collection.obligations
    list.add(new Resource());
  }

  void disposeUserDefined() {
    ResourceList<Resource> list = new ResourceList<>();
    list.add(new Resource());
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }

  void leakNestedJdkClass() {
    ConcurrentHashMap.KeySetView<Resource, Boolean> set = ConcurrentHashMap.newKeySet();
    // :: error: unfulfilled.collection.obligations
    set.add(new Resource());
  }

  void disposeNestedJdkClass() {
    ConcurrentHashMap.KeySetView<Resource, Boolean> set = ConcurrentHashMap.newKeySet();
    set.add(new Resource());
    for (Resource r : set) {
      r.close();
      r.flush();
    }
  }
}
