import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;

import java.util.Comparator;

public class AnonymousClasses {
    private <@H1S1 T extends @H1S1 Comparator<T>> void testGenericAnonymous() {
        // :: error: (type.argument.type.incompatible) :: error: (constructor.invocation.invalid)
        new @H1S1 Gen<T>() {};
        // :: error: (type.argument.type.incompatible) :: warning:
        // (cast.unsafe.constructor.invocation)
        new @H1S1 GenInter<T>() {};
    }
}

class Gen<@H1S2 F extends @H1S2 Object> {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    public @H1S2 Gen() {}
}

interface GenInter<@H1S2 F extends @H1S2 Object> {}

interface Foo {}
