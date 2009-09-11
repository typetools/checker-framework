import edu.umd.cs.findbugs.annotations.*;

public class FindBugs {

    @CheckForNull Object getNull() {
        return null;
    }

    @NonNull MyList<@Nullable Object> getListOfNulls() {
        return null;    // error
    }

    void test() {
        Object o = getNull();
        o.toString();   // error

        MyList<@Nullable Object> l = getListOfNulls();
        l.toString();
        l.get().toString();    // error
    }
}

class MyList<T extends @Nullable Object> {
    T get() { throw new RuntimeException(); }
}