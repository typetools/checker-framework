// When the receiver of a collection mutator is a method call, the error is reported at the call,
// and it can be suppressed there.

import java.util.ArrayList;
import java.util.List;

class CollectionMutatorTempReceiver {

  static List<Resource> make() {
    return new ArrayList<>();
  }

  void unsuppressed() {
    // :: error: unfulfilled.collection.obligations
    make().add(new Resource());
  }

  @SuppressWarnings("unfulfilled.collection.obligations")
  void suppressed() {
    make().add(new Resource());
  }
}
