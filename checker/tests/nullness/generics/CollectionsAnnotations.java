import org.checkerframework.checker.nullness.qual.*;

// This is how I propose the Collection interface be annotated:
interface Collection1<E extends @Nullable Object> {
  public void add(E elt);
}

class PriorityQueue1<E extends @NonNull Object> implements Collection1<E> {
  public void add(E elt) {
    // just to dereference elt
    elt.hashCode();
  }
}

class PriorityQueue2<E extends @NonNull Object> implements Collection1<E> {
  public void add(E elt) {
    // just to dereference elt
    elt.hashCode();
  }
}

// This is how the Collection interface is currently annotated
interface Collection2<E extends @NonNull Object> {
  public void add(E elt);
}

class PriorityQueue3<E extends @NonNull Object> implements Collection2<E> {
  public void add(E elt) {
    // just to dereference elt
    elt.hashCode();
  }
}

class Methods {
  static void addNull1(Collection1 l) {
    // Allowed, because upper bound of Collection1 is Nullable.
    // :: warning: [unchecked] unchecked call to add(E) as a member of the raw type Collection1
    l.add(null);
  }

  static void bad1() {
    addNull1(new PriorityQueue1());
  }

  // If the types are parameterized (as they should be)
  static <@Nullable E extends @Nullable Object> void addNull2(Collection1<E> l) {
    l.add(null);
  }

  static void bad2() {
    // :: error: (type.argument)
    addNull2(new PriorityQueue1<@NonNull Object>());
  }

  public static void main(String[] args) {
    bad2();
  }

  static void bad3() {
    // :: error: (type.argument)
    addNull2(new PriorityQueue2<@NonNull Object>());
  }

  // :: error: (type.argument)
  static <@Nullable E> void addNull3(Collection2<E> l) {
    l.add(null);
  }
}
