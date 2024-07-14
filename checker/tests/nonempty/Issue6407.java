import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.EnsuresNonEmpty;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nonempty.qual.UnknownNonEmpty;

class Issue6407 {

  void usesJdk() {
    // items initially has the type @UnknownNonEmpty
    List<String> items = new LinkedList<>();
    items.add("hello");
    @NonEmpty List<String> bar = items; // OK
    items.remove("hello");
    // :: error: (assignment)
    @NonEmpty List<String> baz = items; // I expect an error here
  }

  static class MyList<E> {
    @SuppressWarnings("contracts.postcondition") // nonfunctional class
    @EnsuresNonEmpty("this")
    boolean add(E e) {
      return true;
    }

    boolean remove(@NonEmpty MyList<E> this, E e) {
      return true;
    }
  }

  <T> boolean removeIt(@NonEmpty MyList<T> myl, T e) {
    return true;
  }

  void noJdk() {
    // items initially has the type @UnknownNonEmpty
    @UnknownNonEmpty MyList<String> items = new MyList<>();
    items.add("hello");
    @NonEmpty MyList<String> bar = items; // OK
    items.remove("hello");
    // :: error: (assignment)
    @NonEmpty MyList<String> baz = items;
  }

  void noJdk2() {
    // items initially has the type @UnknownNonEmpty
    @UnknownNonEmpty MyList<String> items = new MyList<>();
    items.add("hello");
    @NonEmpty MyList<String> bar = items; // OK
    removeIt(items, "hello");
    // :: error: (assignment)
    @NonEmpty MyList<String> baz = items;
  }

  void initialRemoval() {
    // items initially has the type @UnknownNonEmpty
    MyList<String> items = new MyList<>();
    // :: error: (method.invocation)
    items.remove("hello");
  }
}
