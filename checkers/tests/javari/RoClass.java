import checkers.javari.quals.*;

@ReadOnly class RoClass {

    public RoClass() @ReadOnly {} // readonly constructor
    public RoClass(Object s) {}   // "mutable" constructor

    Object x;          // this-mutable resolves as readonly
    static Object sx;  // readonly

    public void testConstructors() {
        Object a = new RoClass();   // error
        @ReadOnly RoClass b = new RoClass();
        a = new RoClass(null);         // error
        b = new RoClass(null);
    }

    public void testFieldsPseudoMutable() {
        @Mutable Object a = null;
        a = x;      // error
        a = sx;     // error
        sx = x;     // error
        x = sx;     // error
    }

    public void testFieldsReadOnly() @ReadOnly {
        @Mutable Object a = null;
        a = x;      // error
        a = sx;     // error
        sx = x;     // error
        x = sx;     // error
    }

}
