package pkg2;

import pkg1.Class1;
import testlib.util.EnsuresOdd;
import testlib.util.Odd;
import testlib.util.RequiresOdd;

public class Class2 {
    @RequiresOdd("Class1.field")
    public void requiresOdd() {
        @Odd Object odd = Class1.field;
    }

    @EnsuresOdd("Class1.field")
    public void ensuresOdd() {
        // :: warning: (cast.unsafe.constructor.invocation)
        Class1.field = new @Odd Object();
    }

    void illegalUse() {
        // :: error: (contracts.precondition.not.satisfied)
        requiresOdd();
    }

    void legalUse() {
        // :: warning: (cast.unsafe.constructor.invocation)
        Class1.field = new @Odd Object();
        requiresOdd();
    }
}
