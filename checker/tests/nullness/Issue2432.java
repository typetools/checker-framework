// Test case for issue 2432:
// https://github.com/typetools/checker-framework/issues/2432

// @skip-test until the issue is fixed

import java.util.List;
import org.checkerframework.checker.nullness.qual.*;

class Issue2432 {

    void foo(List<@PolyNull Object> pl, @Nullable Object obj) {
        pl.add(obj);
    }

    void test1(List<@PolyNull Object> nl, @Nullable Object no) {
        // :: error: (assignment.type.incompatible)
        nl.add(null);
        // :: error: (assignment.type.incompatible)
        nl.add(no);
    }

    void test2(TypeArgClass<@PolyNull Object> tc, @NonNull Object nno, @Nullable Object no) {
        Object obj = tc.echo(no);
        // :: error: (assignment.type.incompatible)
        nno = obj;
    }

    @PolyNull Object foo2(TypeArgClass<@PolyNull Object> pc, @PolyNull Object po, @NonNull Object nno) {
        return pc.add(nno, po);
    }

    void test3(TypeArgClass<@Nullable Object> nc, @Nullable Object no) {
        @NonNull Object nno = new Object();
        // :: error: (assignment.type.incompatible)
        foo2(nc, no, nno);
    }

    private class TypeArgClass<T> {
        @PolyNull Object add(@PolyNull Object obj, T dummy) {
            obj = dummy; // assignment.type.incompatible here?
            return obj;
        }

        T echo(T obj) {
            return obj;
        }

        T echo(T obj, @PolyNull Object dummy) {
            return obj;
        }
    }
}
