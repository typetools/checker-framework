// Unannotated receivers and parameters accept any collection; discarding needs no obligation.

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class NotOwningByDefault {

  static void dispose(@OwningCollection Collection<Resource> c) {
    for (Resource r : c) {
      r.close();
      r.flush();
    }
  }

  static String describe(Object o) {
    return String.valueOf(o);
  }

  static class Node {
    int value() {
      return 1;
    }

    Node self() {
      return this;
    }

    @Override
    public String toString() {
      return "" + value();
    }
  }

  void unannotatedMethods() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    l.hashCode();
    l.spliterator();
    describe(l);
    String.valueOf(l);
    dispose(l);
  }

  void discardingMethods() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    // :: error: method.invocation
    l.removeIf(r -> true);
    // :: error: method.invocation
    l.retainAll(List.of());
    // :: error: method.invocation
    l.replaceAll(r -> r);
    dispose(l);
  }

  // An unannotated parameter is @NotOwningCollection, so it cannot take an owning element.
  void insertOwningIntoUnannotated(Collection<Resource> c) {
    Resource r = new Resource();
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    c.add(r);
    r.close();
    r.flush();
  }

  void discardingWithoutObligation() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    for (Resource r : l) {
      r.close();
      r.flush();
    }
    l.removeIf(r -> true);
  }
}
