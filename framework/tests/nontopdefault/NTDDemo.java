import testlib.nontopdefault.qual.NTDMiddle;

// DefaultQualifierInHierarchy is @NTDMiddle
// DefaultFor receivers is @NTDTop

class NTDDemo {
    Object f;

    Object foo(Object in) {
        @NTDMiddle Object x = in;
        f = in;
        return x;
    }
}
