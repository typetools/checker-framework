package pkg2;

import org.checkerframework.framework.testchecker.util.EnsuresOdd;
import org.checkerframework.framework.testchecker.util.Odd;
import org.checkerframework.framework.testchecker.util.RequiresOdd;

import pkg1.Class1;

public class Class2 {
    @RequiresOdd("Class1.field")
    // :: error: (flowexpr.parse.error)
    public void requiresOddParseError() {
        // :: error: (assignment.type.incompatible)
        @Odd Object odd = Class1.field;
    }

    @RequiresOdd("pkg1.Class1.field")
    public void requiresOdd() {
        @Odd Object odd = Class1.field;
    }

    @EnsuresOdd("Class1.field")
    // :: error: (flowexpr.parse.error)
    public void ensuresOddParseError() {
        // :: warning: (cast.unsafe.constructor.invocation)
        Class1.field = new @Odd Object();
    }

    @EnsuresOdd("pkg1.Class1.field")
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
