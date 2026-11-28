// After a `this(...)` call, the fields hold whatever the other constructor stored in them.  A
// constructor that reassigns such a field afterward discards the field's obligation.

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class ThisConstructorCallFieldState implements Closeable {

  List<Resource> resList;

  ThisConstructorCallFieldState(@OwningCollection List<Resource> l) {
    resList = l;
  }

  ThisConstructorCallFieldState(@Owning Resource r, int unused) {
    resList = new ArrayList<>();
    resList.add(r);
  }

  ThisConstructorCallFieldState(@OwningCollection List<Resource> l, boolean unused) {
    this(l);
    // :: error: unfulfilled.collection.obligations
    resList = new ArrayList<>();
  }

  ThisConstructorCallFieldState(@Owning Resource r) {
    this(r, 0);
    // :: error: unfulfilled.collection.obligations
    resList = new ArrayList<>();
  }

  @Override
  @CollectionFieldDestructor("resList")
  public void close() {
    for (Resource r : resList) {
      r.close();
      r.flush();
    }
  }
}
