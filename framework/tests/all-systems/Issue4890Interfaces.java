@SuppressWarnings("all") // Just check for crashes.
public class Issue4890Interfaces {
    // The capture of BigClass2<Interface1, ? extends SubInterface2<?>> is BigClass<Interface1,
    // cap1>
    // where cap1 is a fresh type variable with upper bound of
    // SubInterface2<?> & Interface2<Interface1> and lower bound of nulltype.
    BigClass2<Interface1, ? extends SubInterface2<?>> r;
    // The type of r.getI2() is cap1 extends SubInterface2<?> & Interface2<Interface1> so
    // both of the following assignments should be legal.
    SubInterface2<?> s = r.getI2();
    // Interface2<Interface1> s2 = r.getI2(); // javac error here, but no error with IntelliJ and
    // Eclipse.

    BigClass2<SubInterface1, ? extends SubInterface2<?>> t;
    SubInterface2<?> s3 = t.getI2();
    // Interface2<SubInterface1> s4 = t.getI2(); // javac error here, but no error with IntelliJ and
    // Eclipse.

    abstract static class BigClass2<I1 extends Interface1, I2 extends Interface2<I1>> {
        abstract I2 getI2();
    }

    interface Interface1 {}

    interface SubInterface1 extends Interface1 {}

    interface Interface2<A extends Interface1> {}

    interface SubInterface2<C extends SubInterface1> extends Interface2<C> {}
}
