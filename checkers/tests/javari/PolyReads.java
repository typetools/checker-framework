import checkers.javari.quals.*;

class PolyReads {

    @Mutable @Assignable Object mObject;
    Object tmObject;
    @ReadOnly Object roObject;

    PolyReads (@PolyRead Object s) @PolyRead {
        mObject = s;           // error, cannot assign to mutable
        tmObject = s;          // assignable at constructor
        roObject = s;          // assignable at constructor

        @PolyRead Object a = s; // ok
        @PolyRead Object b = a; // ok

        a = mObject;           // ok
        a = tmObject;          // ok
        a = roObject;          // error

        mObject = a;           // error
        tmObject = a;          // ok
        roObject = a;          // ok
    }

    @PolyRead Object testAsMutableReceiver(@PolyRead Object s) {
        mObject = s;           // error, cannot assign to mutable
        tmObject = s;          // error, cannot assign to mutable
        roObject = s;          // ok

        @PolyRead Object a = s; // ok
        @PolyRead Object b = a; // ok

        a = mObject;           // ok
        a = tmObject;          // ok
        a = roObject;          // error

        mObject = a;           // error
        tmObject = a;          // error
        roObject = a;          // ok

        return null;
    }

    @PolyRead Object testAsReadOnlyReceiver(@PolyRead Object s) @ReadOnly {
        mObject = s;           // error, s might be readonly
        tmObject = s;          // error, local field, and unassignable (s readonly, mutable class, for example)
        roObject = s;          // error, local field

        @PolyRead Object a = s; // ok
        @PolyRead Object b = a; // ok

        a = mObject;           // ok
        a = tmObject;          // error, a might be mutable
        a = roObject;          // error, a might be mutable

        mObject = a;           // error, a might be readonly
        tmObject = a;          // error, local field, and unassignable (s readonly, mutable class, for example)
        roObject = a;          // error, local field

        return null;
    }

    @PolyRead Object testAsPolyReadReceiver(@PolyRead Object s) @PolyRead {
        mObject = s;           // error, s might be readonly
        tmObject = s;          // error, local field
        roObject = s;          // error, local field

        @PolyRead Object a = s; // ok
        @PolyRead Object b = a; // ok

        a = mObject;           // ok
        a = tmObject;          // ok
        a = roObject;          // error, a might be mutable

        mObject = a;           // error, a might be readonly
        tmObject = a;          // error, local field
        roObject = a;          // error, local field

        return null;
    }



}
