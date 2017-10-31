import org.checkerframework.checker.fenum.qual.Fenum;

@SuppressWarnings("fenum:assignment.type.incompatible")
public class TestInstance {
    public final @Fenum("A") Object ACONST1 = new Object();
    public final @Fenum("A") Object ACONST2 = new Object();
    public final @Fenum("A") Object ACONST3 = new Object();

    public final @Fenum("B") Object BCONST1 = new Object();
    public final @Fenum("B") Object BCONST2 = new Object();
    public final @Fenum("B") Object BCONST3 = new Object();
}

class FenumUserTestInstance {
    @Fenum("A") Object state1 = new TestInstance().ACONST1;

    // :: error: (assignment.type.incompatible)
    @Fenum("B") Object state2 = new TestInstance().ACONST1;

    void foo(TestInstance t) {
        // :: error: (assignment.type.incompatible)
        state1 = new Object();

        state1 = t.ACONST2;
        state1 = t.ACONST3;

        // :: error: (assignment.type.incompatible)
        state1 = t.BCONST1;

        // :: error: (method.invocation.invalid)
        state1.hashCode();
        // :: error: (method.invocation.invalid)
        t.ACONST1.hashCode();

        // sanity check: unqualified instantiation and call work.
        Object o = new Object();
        o.hashCode();

        if (t.ACONST1 == t.ACONST2) {}

        // :: error: (binary.type.incompatible)
        if (t.ACONST1 == t.BCONST2) {}
    }
}
