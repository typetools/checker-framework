// A collection that has its own must-call obligation (here, `shutdown`) in addition to the
// obligations of its elements.  Tests:
//  - passing it to an `@Owning @OwningCollection` parameter transfers both its own obligation and
//    its elements' obligations;
//  - passing it to an `@OwningCollection` parameter transfers only its elements' obligations;
//  - assigning it to an `@OwningCollection` field that is not `@Owning` transfers only its
//    elements' obligations; the local still must satisfy the collection's own obligation.

import java.util.ArrayList;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CollectionWithOwnObligationTransfer {

  @InheritableMustCall("shutdown")
  static class ShutdownList<E extends @MustCallUnknown Object> extends ArrayList<E> {
    void shutdown() {}
  }

  static @OwningCollection ShutdownList<Resource> make() {
    ShutdownList<Resource> list = new ShutdownList<>();
    list.add(new Resource());
    return list;
  }

  static void takeBoth(@Owning @OwningCollection ShutdownList<Resource> list) {
    for (Resource r : list) {
      r.close();
      r.flush();
    }
    list.shutdown();
  }

  static void takeElements(@OwningCollection ShutdownList<Resource> list) {
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }

  void passToBoth() {
    ShutdownList<Resource> list = make();
    takeBoth(list);
  }

  void passToElementsOnly() {
    // :: error: (required.method.not.called)
    ShutdownList<Resource> list = make();
    takeElements(list);
  }

  void passToElementsOnlyThenShutdown() {
    ShutdownList<Resource> list = make();
    takeElements(list);
    list.shutdown();
  }

  @InheritableMustCall("close")
  static class Holder {
    @OwningCollection ShutdownList<Resource> items;

    Holder() {
      // :: error: (required.method.not.called)
      ShutdownList<Resource> list = make();
      items = list;
    }

    @CollectionFieldDestructor("items")
    void close() {
      for (Resource r : items) {
        r.close();
        r.flush();
      }
    }
  }
}
