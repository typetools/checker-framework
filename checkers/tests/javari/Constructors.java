import checkers.javari.quals.*;

/*
 * @skip-test   -  return value of readonly constructors is readonly
 */
class Constructors {

    @Mutable String mString;
    @ReadOnly String roString;

    public @PolyRead Constructors (@PolyRead String s) {}

    public @ReadOnly Constructors (@ReadOnly String s, int x) {}

    public Constructors (String s, String x) {}

    void test() {
        Constructors mc;
        @ReadOnly Constructors roc;

        roc = new Constructors(roString);     // ok
        roc = new Constructors(mString);      // ok
        roc = new Constructors(null);         // ok

        //:: error: (type.incompatible)
        mc = new Constructors(roString);      // cannot assign
        mc = new Constructors(mString);       // ok
        mc = new Constructors(null);          // ok

        roc = new Constructors(roString, 0);  // ok
        roc = new Constructors(mString, 0);   // ok
        roc = new Constructors(null, 0);      // ok

        //:: error: (type.incompatible)
        mc = new Constructors(roString, 0);   // cannot assign
        //:: error: (type.incompatible)
        mc = new Constructors(mString, 0);    // cannot assign
        //:: error: (type.incompatible)
        mc = new Constructors(null, 0);       // cannot assign

        new Constructors(mString, mString);   // ok
        //:: error: (type.incompatible)
        new Constructors(roString, mString);  // illegal parameter
        //:: error: (type.incompatible)
        new Constructors(mString, roString);  // illegal parameter
        //:: error: (type.incompatible)
        new Constructors(roString, roString); // illegal parameters

    }

    ////// Adding test for constructor receivers
    public @Mutable Constructors(@Mutable Constructors o, String i) { }

    public void testConstructorWithReceiver() {
        @ReadOnly Constructors ro = null;
        @Mutable Constructors m = null;
        @Mutable Constructors c1 = new @Mutable Constructors(m, "");
        @Mutable Constructors c2 = new Constructors(m, "");
        //:: error: (type.incompatible)
        @Mutable Constructors c3 = new @ReadOnly Constructors(m, ""); // invalid
        //:: error: (type.incompatible)
        new @ReadOnly Constructors(ro, "");     // illegal parameter
    }
}
