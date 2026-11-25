import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

/*
 * Tests:
 *  - assigning to a component of an array whose component type is @NotOwningCollection does not
 *    move ownership;
 *  - a write to a field that a parameter shadows does not prevent certification of a disposal
 *    loop over the parameter;
 *  - a constructor call can pass an owning collection to a borrowed collection parameter;
 *  - a written @MustCall bound on a borrowed wildcard parameter is not widened;
 *  - a destructor must cover the inherited overloads of the must-call method.
 */
class CollectionOwnershipReviewFixes3 {

  List<String> resources = new ArrayList<>();

  void assignToNotOwningArrayComponent(@NotOwningCollection List<Resource>[] slots) {
    List<Resource> owned = new ArrayList<>();
    owned.add(new Resource());
    slots[0] = owned;
    for (Resource r : owned) {
      r.close();
      r.flush();
    }
  }

  void writeToShadowedField(@OwningCollection List<Resource> resources) {
    for (Resource r : resources) {
      this.resources.set(0, "x");
      r.close();
      r.flush();
    }
  }

  static class Counter {
    Counter(List<?> list) {}
  }

  void passToBorrowingConstructor(@OwningCollection List<Resource> list) {
    new Counter(list);
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }

  static int countWithoutObligations(List<? extends @MustCall({}) Resource> list) {
    return list.size();
  }

  void passToExplicitBound(@OwningCollection List<Resource> list) {
    // :: error: (argument)
    countWithoutObligations(list);
    for (Resource r : list) {
      r.close();
      r.flush();
    }
  }
}

class CloseWithArgument {
  public void close(int unused) {}
}

@InheritableMustCall("close")
class InheritedCloseOverload extends CloseWithArgument {
  // :: error: unfulfilled.field.obligations
  final List<Resource> items = new ArrayList<>();

  @CollectionFieldDestructor("items")
  public void close() {
    for (Resource r : items) {
      r.close();
      r.flush();
    }
  }
}

class CloseWithoutArgument {
  public void close() {}
}

@InheritableMustCall("close")
class OverriddenClose extends CloseWithoutArgument {
  final List<Resource> items = new ArrayList<>();

  @Override
  @CollectionFieldDestructor("items")
  public void close() {
    for (Resource r : items) {
      r.close();
      r.flush();
    }
  }
}
