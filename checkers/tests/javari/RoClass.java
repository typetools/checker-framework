import checkers.javari.quals.*;

/*
 * @skip-test   -  return value of readonly constructors is readonly
 */
@ReadOnly class RoClass {

    public @ReadOnly RoClass() {} // readonly constructor
    public RoClass(Object s) {}   // "mutable" constructor

    Object x;          // this-mutable resolves as readonly
    static Object sx;  // readonly

    public void testConstructors() {
        //:: error: (type.incompatible)
        Object a = new RoClass();   // error
        @ReadOnly RoClass b = new RoClass();
        //:: error: (type.incompatible)
        a = new RoClass(null);         // error
        b = new RoClass(null);
    }

    public void testFieldsPseudoMutable() {
        @Mutable Object a = null;
        //:: error: (type.incompatible)
        a = x;      // error
        //:: error: (type.incompatible)
        a = sx;     // error
        //:: error: (ro.field)
        sx = x;     // error
        //:: error: (ro.field)
        x = sx;     // error
    }

    public void testFieldsReadOnly(@ReadOnly RoClass this) {
        @Mutable Object a = null;
        //:: error: (type.incompatible)
        a = x;      // error
        //:: error: (type.incompatible)
        a = sx;     // error
        //:: error: (ro.field)
        sx = x;     // error
        //:: error: (ro.field)
        x = sx;     // error
    }

}
