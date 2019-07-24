// Test case for issue 2432:
// https://github.com/typetools/checker-framework/issues/2432

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
        Object object = tc.echo(no);
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
        nc.echo(po);
        // :: error: (assignment.type.incompatible)
        po = nc.echo(po);
    }

    @PolyNull Object foo2(TypeArgClass<@PolyNull Object> pc, @PolyNull Object po, @NonNull Object nno) {
        return pc.add(nno, po);
    }

    void fooo(@PolyNull Object o1, @PolyNull Object o2) {}

    void foo2_t(@Nullable Object no, @NonNull Object nno) {
        fooo(no, nno);
        fooo(no, no);
    }

    void test3_1(TypeArgClass<@Nullable Object> nc, @Nullable Object no) {
        @NonNull Object nno = new Object();
        // No error (?)
        foo2(nc, no, nno);
    }

    void test3_2(TypeArgClass<@Nullable Object> nnc, @NonNull Object nno) {
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

    void test4(TypeArgClass<@PolyNull Object> pc, @Nullable Object no, @NonNull Object nno) {
        // :: error: (argument.type.incompatible)
        pc.tripleAdd(no, nno, no);
        // No error
        pc.tripleAdd(no, nno, nno);
    }

    void test4_1(TypeArgClass<@PolyNull Object> pc, @Nullable Object no, @NonNull Object nno) {
        // No error
        pc.echo(nno, no);
    }

    private class TypeArgClass<T> {
        @PolyNull Object add(@PolyNull Object obj, T dummy) {
            return obj;
        }

        @PolyNull Object tripleAdd(@PolyNull Object o1, @PolyNull Object o2, T dummy) {
            return o1;
        }

        T echo(T obj) {
            return obj;
        }

        T echo(T obj, @PolyNull Object dummy) {
            return obj;
        }
    }
}
