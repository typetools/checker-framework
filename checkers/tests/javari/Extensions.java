import checkers.javari.quals.*;

class Extensions {

    class BaseClass {
        String mutableReceiver() {return null;}
        String readonlyReceiver() @ReadOnly {return null;}
        @Mutable String mutableReturn() {return null;}
        @PolyRead String polyReadReturn() @PolyRead {return null;}
        @ReadOnly String readonlyReturn() {return null;}
    }

    @ReadOnly class ReadOnlyEmpty {}

    @ReadOnly class ReadOnlyExtension extends BaseClass {}

    @Mutable class MutableExtension extends BaseClass {}

    //:: error: (polyread.type)
    @PolyRead class PolyReadIllegalExtension extends BaseClass {}

    class SwitchReceivers extends BaseClass {
        String mutableReceiver() @ReadOnly {return null;}
        //:: error: (override.receiver.invalid)
        String readonlyReceiver() @Mutable {return null;} // error
    }

    class SwitchReturns extends BaseClass {
        //:: error: (override.return.invalid)
        @ReadOnly String mutableReturn() {return null;} // error
        @Mutable String readonlyReturn() {return null;}
    }

    class PolyReadReturns extends BaseClass {
        //:: error: (override.return.invalid)
        @PolyRead String mutableReturn() {return null;} // error
        @PolyRead String readonlyReturn() {return null;}
    }

    class ForcePolyReadReturnToMutable extends BaseClass {
        @Mutable String polyReadReturn() @ReadOnly {return null;}
    }

    class ForcePolyReadReturnToReadOnly extends BaseClass {
        //:: error: (override.return.invalid)
        @ReadOnly String polyReadReturn() @ReadOnly {return null;}
    }

    class PolyReadReceivers extends BaseClass {
        String mutableReceiver() @PolyRead {return null;}
        //:: error: (override.receiver.invalid)
        String readonlyReceiver() @PolyRead {return null;} // error
    }

}
