import org.checkerframework.common.aliasing.qual.*;

class UniqueAnnoTest {

    // @Unique constructor
    public @Unique UniqueAnnoTest() {}

    // @Unique constructor leaking the "this" reference.
    // Each unique.leaked error is a leak.
    public @Unique UniqueAnnoTest(int i) {
        notLeaked(this);
        leakedToResult(this);
        // :: error: (unique.leaked)
        UniqueAnnoTest b = leakedToResult(this);

        UniqueAnnoTest other = new UniqueAnnoTest();
        // :: error: (unique.leaked)
        other = this;
        // :: error: (unique.leaked)
        leaked(this);
        // :: error: (unique.leaked)
        leaked(other); // The receiver parameter is "this", so there is a leak.
    }

    // Not @Unique constructor. No warnings.
    public UniqueAnnoTest(int i1, int i2) {
        UniqueAnnoTest other = new UniqueAnnoTest();
        other = this;
        notLeaked(this);
    }

    void leaked(UniqueAnnoTest a) {}

    void notLeaked(@NonLeaked UniqueAnnoTest this, @NonLeaked UniqueAnnoTest a) {}

    UniqueAnnoTest leakedToResult(
            @LeakedToResult UniqueAnnoTest this, @LeakedToResult UniqueAnnoTest a) {
        return a;
    }
}
