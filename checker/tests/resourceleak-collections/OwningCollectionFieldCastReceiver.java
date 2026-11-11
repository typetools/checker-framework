// Inserting into an owning collection field through a cast gives the field an obligation, just as
// inserting into the field directly does, so the field cannot be reassigned afterward.

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;

class OwningCollectionFieldCastReceiver implements Closeable {
  List<Resource> resources = new ArrayList<>();

  OwningCollectionFieldCastReceiver() {
    ((Collection<Resource>) resources).add(new Resource());
    // :: error: unfulfilled.collection.obligations
    resources = new ArrayList<>();
  }

  @Override
  @CollectionFieldDestructor("resources")
  public void close() {
    for (Resource r : resources) {
      r.close();
      r.flush();
    }
  }
}
