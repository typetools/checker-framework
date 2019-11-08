// Test case for issue 2432:
// https://github.com/typetools/checker-framework/issues/2432

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

class Issue2432 {

    void jdkAnnotation(List<@PolyNull Object> nl, @Nullable Object no) {
        // JDK classes are manually annotated in CF
        // :: error: (argument.type.incompatible)
        nl.add(null);
        // :: error: (argument.type.incompatible)
        nl.add(no);
    }

    // receiver's poly annotations in declaration should be different from the ones in invocation
    void polyReceiverType(
            TypeArgClass<@PolyNull Object> tc,
            @NonNull Object nno,
            @Nullable Object no,
            @PolyNull Object po) {
        // :: error: (argument.type.incompatible)
        Object object = tc.echo(no);
        // :: error: (assignment.type.incompatible)
        nno = tc.echo(po);
        // No error. Note po's @PolyNull is undecidable thus not substituted
        no = tc.echo(po);
    }

    void polyAssignment(
            TypeArgClass<@NonNull Object> nnc,
            @NonNull Object nno,
            @Nullable Object no,
            @PolyNull Object po) {
        // :: error: (argument.type.incompatible)
        nnc.echo(po);
    }

    void polyAssignment2(
            TypeArgClass<@Nullable Object> nc,
            @NonNull Object nno,
            @Nullable Object no,
            @PolyNull Object po) {
        // No error
        nc.echo(po);
        // :: error: (assignment.type.incompatible)
        po = nc.echo(po);
    }

    // a "foo function" with 2 poly annotations, where one of them appears in type argument
    // purpose: test invocation without a receiver
    @PolyNull Object foo2PolyTypeArg(
            TypeArgClass<@PolyNull Object> pc, @PolyNull Object po, @NonNull Object nno) {
        return pc.add(nno, po);
    }

    // lub test without receiver
    // 4 tests corresponds to different lub combination
    void lubWithTypeArgNoReceiver1(TypeArgClass<@Nullable Object> nc, @Nullable Object no) {
        @NonNull Object nno = new Object();
        // No error
        foo2PolyTypeArg(nc, no, nno);
    }

    void lubWithTypeArgNoReceiver2(TypeArgClass<@Nullable Object> nnc, @NonNull Object nno) {
        // No error
        foo2PolyTypeArg(nnc, nno, nno);
    }

    void lubWithTypeArgNoReceiver3(TypeArgClass<@Nullable Object> nc, @NonNull Object nno) {
        // No error
        foo2PolyTypeArg(nc, nno, nno);
    }

    void lubWithTypeArgNoReceiver4(TypeArgClass<@NonNull Object> nnc, @Nullable Object no) {
        // :: error: (argument.type.incompatible)
        foo2PolyTypeArg(nnc, no, new Object());
    }

    // lub test with receiver
    // T dummy in tripleAdd is to ensure poly annotations from declaration is handled serperately
    void lubWithReceiver(
            TypeArgClass<@PolyNull Object> pc, @Nullable Object no, @NonNull Object nno) {
        // :: error: (argument.type.incompatible)
        pc.tripleAdd(no, nno, no);
        // No error
        pc.tripleAdd(no, nno, nno);
    }

    // ensure ensure poly annotations from declaration is handled serperately from poly from other
    // context
    void declarationPolyInParameter(
            TypeArgClass<@PolyNull Object> pc, @Nullable Object no, @NonNull Object nno) {
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
