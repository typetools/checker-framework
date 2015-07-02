import org.checkerframework.framework.qual.DefaultQualifiers;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultLocation;

import polyall.quals.*;

// Test defaulting behavior, e.g. that local variables, casts, and instanceof
// propagate the type of the respective sub-expression and that upper bounds
// are separately annotated.
class Defaulting {

    @DefaultQualifier(value=H1S1.class, locations={DefaultLocation.LOCAL_VARIABLE})
    class TestLocal {
        void m(@H1S1 Object p1, @H1S2 Object p2) {
            Object l1 = p1;
            //:: error: (assignment.type.incompatible)
            Object l2 = p2;
        }
    }

    @DefaultQualifiers({
        @DefaultQualifier(value=H1Top.class, locations={DefaultLocation.LOCAL_VARIABLE}),
        @DefaultQualifier(value=H1S1.class, locations={DefaultLocation.UPPER_BOUNDS}),
        @DefaultQualifier(value=H1S2.class, locations={DefaultLocation.OTHERWISE})
    })
    //Type of x is <@H1S2 X extends @H1S1 Object>, these annotations are siblings
    //and should not be in the same bound
    //:: error: (bound.type.incompatible)
    class TestUpperBound<X extends Object> {
        void m(X p) {
            @H1S1 Object l1 = p;
            //:: error: (assignment.type.incompatible)
            @H1S2 Object l2 = p;
            Object l3 = p;
        }
    }

    @DefaultQualifiers({
        @DefaultQualifier(value=H1Top.class, locations={DefaultLocation.LOCAL_VARIABLE}),
        @DefaultQualifier(value=H1S1.class, locations={DefaultLocation.PARAMETERS}),
        @DefaultQualifier(value=H1S2.class, locations={DefaultLocation.OTHERWISE})
    })
    class TestParameter {
        void m(Object p) {
            @H1S1 Object l1 = p;
            //:: error: (assignment.type.incompatible)
            @H1S2 Object l2 = p;
            Object l3 = p;
        }
        void call() {
            m(new @H1S1 Object());
            //:: error: (argument.type.incompatible)
            m(new @H1S2 Object());
            //:: error: (argument.type.incompatible)
            m(new Object());
        }
    }
    @DefaultQualifiers({
        @DefaultQualifier(value=H1Top.class, locations={DefaultLocation.LOCAL_VARIABLE}),
        @DefaultQualifier(value=H1S1.class, locations={DefaultLocation.PARAMETERS}),
        @DefaultQualifier(value=H1S2.class, locations={DefaultLocation.OTHERWISE})
    })
    class TestConstructorParameter {

        TestConstructorParameter (Object p) {
            @H1S1 Object l1 = p;
            //:: error: (assignment.type.incompatible)
            @H1S2 Object l2 = p;
            Object l3 = p;
        }
        void call() {
            new TestConstructorParameter(new @H1S1 Object());
            //:: error: (argument.type.incompatible)
            new TestConstructorParameter(new @H1S2 Object());
            //:: error: (argument.type.incompatible)
            new TestConstructorParameter(new Object());
        }
    }

    @DefaultQualifiers({
        @DefaultQualifier(value=H1Top.class, locations={DefaultLocation.LOCAL_VARIABLE}),
        @DefaultQualifier(value=H1S1.class, locations={DefaultLocation.RETURNS}),
        @DefaultQualifier(value=H1S2.class, locations={DefaultLocation.OTHERWISE})
    })
    class TestReturns {
        Object res() {
            return new @H1S1 Object();
        }

        void m() {
            @H1S1 Object l1 = res();
            //:: error: (assignment.type.incompatible)
            @H1S2 Object l2 = res();
            Object l3 = res();
        }

        Object res2() {
            //:: error: (return.type.incompatible)
            return new @H1S2 Object();
        }

        Object res3() {
            //:: error: (return.type.incompatible)
            return new Object();
        }
    }

    @DefaultQualifiers({
        @DefaultQualifier(value=H1Top.class, locations={DefaultLocation.LOCAL_VARIABLE}),
        @DefaultQualifier(value=H1S1.class, locations={DefaultLocation.RECEIVERS})
    })
    public class ReceiverDefaulting {
        public ReceiverDefaulting() {};
        public void m() {}
    }

    @DefaultQualifiers({
        @DefaultQualifier(value=H1Top.class, locations={DefaultLocation.LOCAL_VARIABLE}),
    })
    class TestReceiver {

        void call() {
            @H1S1 ReceiverDefaulting r2 =  new @H1S1 ReceiverDefaulting();
            @H1S2 ReceiverDefaulting r3 = new @H1S2 ReceiverDefaulting();
            ReceiverDefaulting r = new ReceiverDefaulting();

            r2.m();
            //:: error: (method.invocation.invalid)
            r3.m();
            //:: error: (method.invocation.invalid)
            r.m();
        }
    }

}
