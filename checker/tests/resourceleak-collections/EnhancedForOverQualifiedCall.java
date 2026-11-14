// An enhanced `for` loop over the result of a method call is a disposal loop, whether or not the
// call has an explicit receiver.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class EnhancedForOverQualifiedCall {

  @OwningCollection
  List<Resource> fresh() {
    List<Resource> l = new ArrayList<>();
    l.add(new Resource());
    return l;
  }

  void unqualified() {
    for (Resource r : fresh()) {
      r.close();
      r.flush();
    }
  }

  void qualifiedByThis() {
    for (Resource r : this.fresh()) {
      r.close();
      r.flush();
    }
  }

  void qualifiedByOtherObject(EnhancedForOverQualifiedCall other) {
    for (Resource r : other.fresh()) {
      r.close();
      r.flush();
    }
  }
}
