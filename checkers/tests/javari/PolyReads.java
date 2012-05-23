import checkers.javari.quals.*;

class PolyReads {

    @Mutable @Assignable Object mObject;
    Object tmObject;
    @ReadOnly Object roObject;

    PolyReads (@PolyRead Object s) {
        //:: error: (assignment.type.incompatible)
        mObject = s;           // error, cannot assign to mutable
        //:: error: (assignment.type.incompatible)
        tmObject = s;          // error, cannot assign to thismutable TODO?
        roObject = s;          // assignable at constructor

        @PolyRead Object a = s; // ok
        @PolyRead Object b = a; // ok

        a = mObject;           // ok
        a = tmObject;          // ok
        //:: error: (assignment.type.incompatible)
        a = roObject;          // error

        //:: error: (assignment.type.incompatible)
        mObject = a;           // error
        //:: error: (assignment.type.incompatible)
        tmObject = a;          // error, cannot assign to thismutable TODO?
        roObject = a;          // ok
    }

    @PolyRead Object testAsMutableReceiver(@PolyRead Object s) {
        //:: error: (assignment.type.incompatible)
        mObject = s;           // error, cannot assign to mutable
        //:: error: (assignment.type.incompatible)
        tmObject = s;          // error, cannot assign to mutable
        roObject = s;          // ok

        @PolyRead Object a = s; // ok
        @PolyRead Object b = a; // ok

        a = mObject;           // ok
        a = tmObject;          // ok
        //:: error: (assignment.type.incompatible)
        a = roObject;          // error

        //:: error: (assignment.type.incompatible)
        mObject = a;           // error
        //:: error: (assignment.type.incompatible)
        tmObject = a;          // error
        roObject = a;          // ok

        return null;
    }

    @PolyRead Object testAsReadOnlyReceiver(@ReadOnly PolyReads this, @PolyRead Object s) {
        //:: error: (assignment.type.incompatible)
        mObject = s;           // error, s might be readonly
        //:: error: (assignment.type.incompatible) :: error: (ro.field)
        tmObject = s;          // error, local field, and unassignable (s readonly, mutable class, for example)
        //:: error: (ro.field)
        roObject = s;          // error, local field

        @PolyRead Object a = s; // ok
        @PolyRead Object b = a; // ok

        a = mObject;           // ok
        //:: error: (assignment.type.incompatible)
        a = tmObject;          // error, a might be mutable
        //:: error: (assignment.type.incompatible)
        a = roObject;          // error, a might be mutable

        //:: error: (assignment.type.incompatible)
        mObject = a;           // error, a might be readonly
        //:: error: (assignment.type.incompatible) :: error: (ro.field)
        tmObject = a;          // error, local field, and unassignable (s readonly, mutable class, for example)
        //:: error: (ro.field)
        roObject = a;          // error, local field

        return null;
    }

    @PolyRead Object testAsPolyReadReceiver(@PolyRead PolyReads this, @PolyRead Object s) {
        //:: error: (assignment.type.incompatible)
        mObject = s;           // error, s might be readonly
        //:: error: (ro.field)
        tmObject = s;          // error, local field
        //:: error: (ro.field)
        roObject = s;          // error, local field

        @PolyRead Object a = s; // ok
        @PolyRead Object b = a; // ok

        a = mObject;           // ok
        a = tmObject;          // ok
        //:: error: (assignment.type.incompatible)
        a = roObject;          // error, a might be mutable

        //:: error: (assignment.type.incompatible)
        mObject = a;           // error, a might be readonly
        //:: error: (ro.field)
        tmObject = a;          // error, local field
        //:: error: (ro.field)
        roObject = a;          // error, local field

        return null;
    }



}
