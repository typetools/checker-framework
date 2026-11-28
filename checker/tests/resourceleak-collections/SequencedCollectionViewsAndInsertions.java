// A reversed list view borrows elements, and a borrowed list cannot accept ownership via
// `addFirst`.

// @below-java21-jdk-skip-test

import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.Owning;

class SequencedCollectionViewsAndInsertions {

  // A reversed list view does not own its elements.
  @OwningCollection
  List<Resource> reversedViewDoesNotOwnElements(List<Resource> c) {
    // :: error: return
    return c.reversed();
  }

  // `List.addFirst` cannot transfer ownership into a borrowed list.
  void addFirstIntoBorrowedList(List<Resource> c, @Owning Resource r) {
    // :: error: illegal.collection.mutator.owning.insert.into.notowning
    c.addFirst(r);
    r.flush();
    r.close();
  }
}
