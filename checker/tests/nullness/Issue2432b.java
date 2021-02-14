// Ensure correct handling of type parameters and arrays.
// https://github.com/typetools/checker-framework/issues/2432

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue2432b {
    void objectAsTypeArg() {
        List<Object> objs = new ArrayList<>();
        // no error
        Object[] objarray = objs.toArray();
    }

    void myClassAsTypeArg() {
        MyClass<Object> objs = new MyClass<>();
        Object[] objarray = objs.toArray();
        // no error
        Object[] objarray2 = objs.toArrayPoly();
    }

    void stringAsTypeArg() {
        List<String> strs = new ArrayList<>();
        Object[] strarray = strs.toArray();
    }

    void listAsTypeArg() {
        List<List> lists = new ArrayList<>();
        Object[] listarray = lists.toArray();
    }

    private static class MyClass<MyTypeParam> {

        Object[] toArray() {
            return new Object[] {new Object()};
        }

        @PolyNull Object[] toArrayPoly(MyClass<@PolyNull MyTypeParam> this) {
            return new Object[] {new Object()};
        }
    }
}
