import edu.umd.cs.findbugs.annotations.*;

public class FindBugs {

    @CheckForNull Object getNull() {
        return null;
    }

    @NonNull MyList<@Nullable Object> getListOfNulls() {
        //:: error: (return.type.incompatible)
        return null;    // error
    }

    void test() {
        Object o = getNull();
        //:: error: (dereference.of.nullable)
        o.toString();   // error

        MyList<@Nullable Object> l = getListOfNulls();
        l.toString();
        //:: error: (dereference.of.nullable)
        l.get().toString();    // error
    }
}

class MyList<T extends @Nullable Object> {
    T get() { throw new RuntimeException(); }
}