// A call to any overload of a must-call method satisfies the obligation, including an overload
// that the class inherits.  So a must-call method destructs a collection field only if every
// overload with its name, declared or inherited, is a @CollectionFieldDestructor for the field.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CollectionFieldDestructorInheritedOverload {

  static class BaseWithCloseInt {
    public void close(int unused) {}
  }

  // A caller can satisfy the obligation by calling the inherited close(int), which does not
  // dispose of the elements of `items`.
  @InheritableMustCall("close")
  static class InheritsNonDestructorOverload extends BaseWithCloseInt {
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

  static class BaseWithClose {
    public void close() {}
  }

  // The destructor overrides the only inherited close(), so every callable close() is the
  // destructor.
  @InheritableMustCall("close")
  static class OverridesInheritedClose extends BaseWithClose {
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

  @InheritableMustCall("close")
  interface Closer {
    void close();
  }

  // The destructor implements Closer.close(), so every callable close() is the destructor.
  static class ImplementsCloser implements Closer {
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
}
