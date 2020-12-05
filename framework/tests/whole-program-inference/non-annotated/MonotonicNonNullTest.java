// TODO: This does not test inference of @MonotonicNonNull because the WPI tests run the Value
// Checker, not the Nullness Checker.

class MonotonicNonNull {

    String nble1;

    String nble2 = null;

    String nn = "a";

    String mnn1;

    String mnn2 = null;

    void foo() {
        nble1 = null;
        nble2 = null;
        nn = "hello";
        mnn1 = "hello";
        mnn2 = "hello";
    }
}
