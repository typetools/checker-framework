import checkers.javari.quals.*;

class Extensions {

    class BaseClass {
        String mutableReceiver() {return null;}
        String readonlyReceiver(@ReadOnly BaseClass this) {return null;}
        @Mutable String mutableReturn() {return null;}
        @PolyRead String polyReadReturn(@PolyRead BaseClass this) {return null;}
        @ReadOnly String readonlyReturn() {return null;}
    }

    @ReadOnly class ReadOnlyEmpty {}

    @ReadOnly class ReadOnlyExtension extends BaseClass {}

    @Mutable class MutableExtension extends BaseClass {}

    //:: error: (polyread.type)
    @PolyRead class PolyReadIllegalExtension extends BaseClass {}

    class SwitchReceivers extends BaseClass {
        String mutableReceiver(@ReadOnly SwitchReceivers this) {return null;}
        //:: error: (override.receiver.invalid)
        String readonlyReceiver(@Mutable SwitchReceivers this) {return null;} // error
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
        @Mutable String polyReadReturn(@ReadOnly ForcePolyReadReturnToMutable this) {return null;}
    }

    class ForcePolyReadReturnToReadOnly extends BaseClass {
        //:: error: (override.return.invalid)
        @ReadOnly String polyReadReturn(@ReadOnly ForcePolyReadReturnToReadOnly this) {return null;}
    }

    class PolyReadReceivers extends BaseClass {
        String mutableReceiver(@PolyRead PolyReadReceivers this) {return null;}
        //:: error: (override.receiver.invalid)
        String readonlyReceiver(@PolyRead PolyReadReceivers this) {return null;} // error
    }

}
