// Methods that discard collection elements require their resources to be disposed first.

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;
import org.checkerframework.checker.collectionownership.qual.*;

class DiscardingMethodsRequireDisposal {

  static void dispose(@OwningCollection Collection<Resource> c) {
    for (Resource r : c) {
      r.close();
      r.flush();
    }
  }

  // `Vector.setSize` cannot discard resources before disposal.
  void setSizeBeforeDisposal() {
    Vector<Resource> v = new Vector<>();
    v.add(new Resource());
    // :: error: method.invocation
    v.setSize(0);
    dispose(v);
  }

  // `Vector.setSize` can discard elements after disposal.
  void setSizeAfterDisposal() {
    Vector<Resource> v = new Vector<>();
    v.add(new Resource());
    for (Resource r : v) {
      r.close();
      r.flush();
    }
    v.setSize(0);
  }

  static class Pool extends ArrayList<Resource> {
    // `removeRange` cannot discard resources before disposal.
    void removeRangeBeforeDisposal(@OwningCollection Pool this) {
      // :: error: method.invocation
      this.removeRange(0, 1);
      for (Resource r : this) {
        r.close();
        r.flush();
      }
    }

    // `removeRange` can discard elements after disposal.
    void removeRangeAfterDisposal(@OwningCollection Pool this) {
      for (Resource r : this) {
        r.close();
        r.flush();
      }
      this.removeRange(0, 1);
    }

    // An implicit receiver still reports an error after disposal.
    void removeRangeAfterDisposalImplicitReceiver(@OwningCollection Pool this) {
      for (Resource r : this) {
        r.close();
        r.flush();
      }
      // False positive: the disposal loop does not refine the type of an implicit receiver.
      // :: error: method.invocation
      removeRange(0, 1);
    }
  }
}
