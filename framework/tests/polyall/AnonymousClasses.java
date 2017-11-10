import java.util.Comparator;
import polyall.quals.H1S1;
import polyall.quals.H1S2;

class AnonymousClasses {
    private <@H1S1 T extends @H1S1 Comparator<T>> void testGenericAnonymous() {
        // :: error: (type.argument.type.incompatible) :: error: (constructor.invocation.invalid)
        new @H1S1 Gen<T>() {};
        // :: error: (type.argument.type.incompatible)
        new @H1S1 GenInter<T>() {};
    }
}

class Gen<@H1S2 F extends @H1S2 Object> {
    public @H1S2 Gen() {}
}

interface GenInter<@H1S2 F extends @H1S2 Object> {}

interface Foo {}
