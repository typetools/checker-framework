import edu.umd.cs.findbugs.annotations.*;

public class FindBugs {

  @CheckForNull
  Object getNull() {
    return null;
  }

  @NonNull MyList<@org.checkerframework.checker.nullness.qual.Nullable Object> getListOfNulls() {
    // :: error: (return)
    return null; // error
  }

  void test() {
    Object o = getNull();
    // :: error: (dereference.of.nullable)
    o.toString(); // error

    MyList<@org.checkerframework.checker.nullness.qual.Nullable Object> l = getListOfNulls();
    l.toString();
    // :: error: (dereference.of.nullable)
    l.get().toString(); // error
  }
}

class MyList<T extends @org.checkerframework.checker.nullness.qual.Nullable Object> {
  T get() {
    throw new RuntimeException();
  }
}
