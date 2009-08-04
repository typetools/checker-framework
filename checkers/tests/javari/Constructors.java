import checkers.javari.quals.*;

class Constructors {

    @Mutable String mString;
    @ReadOnly String roString;

    public Constructors (@PolyRead String s) @PolyRead {}

    public Constructors (@ReadOnly String s, int x) @ReadOnly{}

    public Constructors (String s, String x) {}

    void test() {
        Constructors mc;
        @ReadOnly Constructors roc;

        roc = new Constructors(roString);     // ok
        roc = new Constructors(mString);      // ok
        roc = new Constructors(null);         // ok

        mc = new Constructors(roString);      // cannot assign
        mc = new Constructors(mString);       // ok
        mc = new Constructors(null);          // ok

        roc = new Constructors(roString, 0);  // ok
        roc = new Constructors(mString, 0);   // ok
        roc = new Constructors(null, 0);      // ok

        mc = new Constructors(roString, 0);   // cannot assign
        mc = new Constructors(mString, 0);    // cannot assign
        mc = new Constructors(null, 0);       // cannot assign

        new Constructors(mString, mString);   // ok
        new Constructors(roString, mString);  // illegal parameter
        new Constructors(mString, roString);  // illegal parameter
        new Constructors(roString, roString); // illegal parameters

    }

    ////// Adding test for constructor receivers
    public Constructors(@Mutable Constructors o, String i) @Mutable { }

    public void testConstructorWithReceiver() {
        @ReadOnly Constructors ro = null;
        @Mutable Constructors m = null;
        @Mutable Constructors c1 = new @Mutable Constructors(m, "");
        @Mutable Constructors c2 = new Constructors(m, "");
        @Mutable Constructors c3 = new @ReadOnly Constructors(m, ""); // invalid
        new @ReadOnly Constructors(ro, "");     // illegal parameter
    }
}
