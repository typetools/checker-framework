import checkers.javari.quals.*;

@ReadOnly class RoClass {

    public RoClass() @ReadOnly {} // readonly constructor
    public RoClass(Object s) {}   // "mutable" constructor

    Object x;          // this-mutable resolves as readonly
    static Object sx;  // readonly

    public void testConstructors() {
        //:: (type.incompatible)
        Object a = new RoClass();   // error
        @ReadOnly RoClass b = new RoClass();
        //:: (type.incompatible)
        a = new RoClass(null);         // error
        b = new RoClass(null);
    }

    public void testFieldsPseudoMutable() {
        @Mutable Object a = null;
        //:: (type.incompatible)
        a = x;      // error
        //:: (type.incompatible)
        a = sx;     // error
        //:: (ro.field)
        sx = x;     // error
        //:: (ro.field)
        x = sx;     // error
    }

    public void testFieldsReadOnly() @ReadOnly {
        @Mutable Object a = null;
        //:: (type.incompatible)
        a = x;      // error
        //:: (type.incompatible)
        a = sx;     // error
        //:: (ro.field)
        sx = x;     // error
        //:: (ro.field)
        x = sx;     // error
    }

}
