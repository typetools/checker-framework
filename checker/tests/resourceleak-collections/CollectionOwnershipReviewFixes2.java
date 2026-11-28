import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests:
 *  - a self-assignment does not move ownership;
 *  - an assignment to an unrelated field with the same name as the collection does not prevent
 *    certification of a disposal loop;
 *  - the elements of a loop are disposed of through an alias after the loop variable is
 *    overwritten;
 *  - a destructor must be annotated on every overload of the must-call method.
 */
class CollectionOwnershipReviewFixes2 {

  static class Stats {
    int resources;
  }

  void insertParam(@NotOwningCollection List<Resource> borrowed, Resource r) {
    borrowed.add(r);
  }

  void selfAssignment() {
    List<Resource> list = new ArrayList<>();
    list = list;
    list.add(new Resource());
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }

  void assignUnrelatedField(@OwningCollection List<Resource> resources, Stats stats) {
    for (Resource r : resources) {
      stats.resources = 1;
      r.close();
      r.flush();
    }
  }

  void disposeThroughAliasAfterOverwrite(
      @OwningCollection List<Resource> resources, Resource other) {
    for (Resource r : resources) {
      r.flush();
      Resource saved = r;
      r = other;
      saved.close();
    }
  }
}

@InheritableMustCall("close")
class OverloadedCollectionFieldDestructor {
  // :: error: unfulfilled.field.obligations
  final List<Resource> items = new ArrayList<>();

  public void close() {}

  @CollectionFieldDestructor("items")
  public void close(int unused) {
    for (Resource r : items) {
      r.close();
      r.flush();
    }
  }
}

@InheritableMustCall("close")
class AllOverloadsCollectionFieldDestructor {
  final List<Resource> items = new ArrayList<>();

  @CollectionFieldDestructor("items")
  public void close() {
    for (Resource r : items) {
      r.close();
      r.flush();
    }
  }

  @CollectionFieldDestructor("this.items")
  public void close(int unused) {
    close();
  }
}
