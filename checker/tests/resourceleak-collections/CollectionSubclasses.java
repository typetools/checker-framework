// A subclass of a JDK collection class is a collection, just like the JDK class.

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.collectionownership.qual.*;

// TODO: The checker does not treat a user-defined collection class, or a nested JDK collection
// class such as `ConcurrentHashMap.KeySetView`, as a collection.  ResourceLeakUtils.isCollection
// calls Class.forName on the class's canonical name, which fails for a class that is not on the
// checker's own classpath and for a nested class (whose binary name uses `$`).  When this is fixed,
// each method below should behave like the corresponding method for ArrayList:  the leaking
// methods should get only an unfulfilled.collection.obligations error, and the disposing method
// should get no error.
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
    // :: error: type.argument
    // :: error: type.arguments.not.inferred
    ResourceList<Resource> list = new ResourceList<>();
    // :: error: required.method.not.called
    list.add(new Resource());
  }

  void disposeUserDefined() {
    // :: error: type.argument
    // :: error: type.arguments.not.inferred
    ResourceList<Resource> list = new ResourceList<>();
    // :: error: required.method.not.called
    list.add(new Resource());
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }

  void leakNestedJdkClass() {
    // :: error: type.argument
    // :: error: type.arguments.not.inferred
    ConcurrentHashMap.KeySetView<Resource, Boolean> set = ConcurrentHashMap.newKeySet();
    // :: error: required.method.not.called
    set.add(new Resource());
  }
}
