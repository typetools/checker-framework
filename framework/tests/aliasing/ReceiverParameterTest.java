import org.checkerframework.common.aliasing.qual.*;

public class ReceiverParameterTest {

    public @Unique ReceiverParameterTest() {
        nonLeaked();
        // :: error: (unique.leaked)
        mayLeak();
    }

    public @Unique ReceiverParameterTest(int i) {
        leakedToResult();
        // :: error: (unique.leaked)
        ReceiverParameterTest b = leakedToResult();
    }

    public @Unique ReceiverParameterTest(String s) {}

    void receiverTest() {
        ReceiverParameterTest rec = new ReceiverParameterTest("s"); // @Unique
        isUnique(rec);
        rec.leakedToResult();
        isUnique(rec);
        ReceiverParameterTest other = rec.leakedToResult();
        // :: error: (argument.type.incompatible)
        isUnique(rec);
        // :: error: (argument.type.incompatible)
        isUnique(other);
    }

    void stubFileReceiverTest() {
        // StringBuffer append(String s) @LeakedToResult;
        StringBuffer sb = new StringBuffer();
        isUnique(sb);
        sb.append("something");
        isUnique(sb);
        StringBuffer sb2 = sb.append("something");
        // :: error: (argument.type.incompatible)
        isUnique(sb);
        // :: error: (argument.type.incompatible)
        isUnique(sb2);
    }

    ReceiverParameterTest leakedToResult(@LeakedToResult ReceiverParameterTest this) {
        return this;
    }

    void nonLeaked(@NonLeaked ReceiverParameterTest this) {}

    void mayLeak() {}

    // @NonLeaked so it doesn't refine the type of the argument.
    void isUnique(@NonLeaked @Unique ReceiverParameterTest s) {}
    // @NonLeaked so it doesn't refine the type of the argument.
    void isUnique(@NonLeaked @Unique String s) {}
    // @NonLeaked so it doesn't refine the type of the argument.
    void isUnique(@NonLeaked @Unique StringBuffer s) {}
}
