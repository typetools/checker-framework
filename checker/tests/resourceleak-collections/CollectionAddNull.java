// Inserting a null literal into a resource collection inserts no resource, so it creates no
// obligation, and it is permitted even into a non-owning collection.

import java.util.ArrayList;
import java.util.List;

class CollectionAddNull {

  void addNullToNotOwning(List<Resource> notOwning) {
    notOwning.add(null);
  }

  void addNullToOwning() {
    List<Resource> owning = new ArrayList<>();
    owning.add(null);
  }
}
