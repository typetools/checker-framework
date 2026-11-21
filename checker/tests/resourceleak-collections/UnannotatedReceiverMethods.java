// Methods that neither insert nor discard elements can be called on any resource collection.

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class UnannotatedReceiverMethods {

  static void dispose(@OwningCollection Collection<Resource> c) {
    for (Resource r : c) {
      r.close();
      r.flush();
    }
  }

  void owning() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    System.out.println(l.toString());
    Class<?> k = l.getClass();
    l.sort(null);
    List<Resource> view = l.subList(0, l.size());
    dispose(l);
  }

  void notOwning(List<Resource> p) {
    System.out.println(p.toString());
    Class<?> k = p.getClass();
  }
}
