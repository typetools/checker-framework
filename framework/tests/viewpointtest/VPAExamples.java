import viewpointtest.quals.*;

public class VPAExamples {

    static class RDContainer {
        @ReceiverDependentQual
        Object get() {
            return null;
        }

        void set(@ReceiverDependentQual Object o) {}

        @ReceiverDependentQual Object field;
    }

    void tests(@A RDContainer a, @B RDContainer b, @Top RDContainer top) {
        @A Object aObj = a.get();
        @B Object bObj = b.get();
        @Top Object tObj = top.get();
        // :: error: (assignment.type.incompatible)
        bObj = a.get();
        // :: error: (assignment.type.incompatible)
        aObj = top.get();
        // :: error: (assignment.type.incompatible)
        bObj = top.get();

        a.set(aObj);
        // :: error: (argument.type.incompatible)
        a.set(bObj);
        // :: error: (argument.type.incompatible)
        b.set(aObj);
        b.set(bObj);
        top.set(aObj);
        top.set(bObj);
    }
}
