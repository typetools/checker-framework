// The field name in an inherited @CollectionFieldDestructor annotation refers to a field of the
// class that declares the destructor, not to a subclass field that hides it.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CollectionFieldDestructorHiddenField {

  @InheritableMustCall("close")
  static class Parent {
    final List<Resource> items = new ArrayList<>();

    @CollectionFieldDestructor("items")
    public void close() {
      for (Resource r : items) {
        r.close();
        r.flush();
      }
    }
  }

  // Parent.close() disposes of the elements of Parent.items, not of Child.items.
  static class Child extends Parent {
    // :: error: unfulfilled.field.obligations
    final List<Resource> items = new ArrayList<>();
  }

  // The inherited destructor disposes of the inherited field, which is not hidden.
  static class ChildWithoutHiding extends Parent {}
}
