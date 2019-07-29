// Test case for issue 2432:
// https://github.com/typetools/checker-framework/issues/2432

// @skip-test until the issue is fixed

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

class Issue2432 {

    void test1(List<@PolyNull Object> nl, @Nullable Object no) {
        // :: error: (argument.type.incompatible)
        nl.add(null);
        // :: error: (argument.type.incompatible)
        nl.add(no);
    }

    void test2_1(
            TypeArgClass<@PolyNull Object> tc,
            @NonNull Object nno,
            @Nullable Object no,
            @PolyNull Object po) {
        // :: error: (argument.type.incompatible)
        nno = tc.echo(no);
        // :: error: (assignment.type.incompatible)
        nno = tc.echo(po);
        // No error
        no = tc.echo(po);
    }

    void test2_2(
            TypeArgClass<@NonNull Object> nnc,
            @NonNull Object nno,
            @Nullable Object no,
            @PolyNull Object po) {
        // :: error: (argument.type.incompatible)
        nnc.echo(po);
    }

    void test2_3(
            TypeArgClass<@Nullable Object> nc,
            @NonNull Object nno,
            @Nullable Object no,
            @PolyNull Object po) {
        // No error
        nnc.echo(po);
        // :: error: (assignment.type.incompatible)
        po = nnc.echo(po);
    }

    @PolyNull Object foo2(TypeArgClass<@PolyNull Object> pc, @PolyNull Object po, @NonNull Object nno) {
        return pc.add(nno, po);
    }

    void test3_1(TypeArgClass<@Nullable Object> nc, @Nullable Object no) {
        @NonNull Object nno = new Object();
        // No error
        foo2(nc, no, nno);
    }

    void test3_2(TypeArgClass<@NonNull Object> nnc, @NonNull Object nno) {
        // No error
        foo2(nnc, nno, nno);
    }

    void test3_3(TypeArgClass<@Nullable Object> nc, @NonNull Object nno) {
        // No error
        foo2(nc, nno, nno);
    }

    void test3_4(TypeArgClass<@NonNull Object> nnc, @Nullable Object no) {
        // :: error: (argument.type.incompatible)
        foo2(nnc, no, new Object());
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
