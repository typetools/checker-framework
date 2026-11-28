// An element of a resource collection can itself be a resource collection.  A call such as
// `add` inserts it as a single element, not its elements.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CollectionOfResourceCollections {

  @InheritableMustCall("close")
  static class Bucket extends ArrayList<Resource> {
    void close() {}
  }

  // `add` transfers the obligation of `bucket` to `buckets`, and the loop satisfies it.
  void addBucket() {
    List<Bucket> buckets = new ArrayList<>();
    Bucket bucket = new Bucket();
    buckets.add(bucket);
    for (Bucket b : buckets) {
      b.close();
    }
  }

  // `add` inserts an owning bucket into a borrowed collection.
  void addBucketToBorrowed(@NotOwningCollection List<Bucket> buckets) {
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    // :: error: required.method.not.called
    buckets.add(new Bucket());
  }
}
