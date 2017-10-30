package defaulting.lowerbound;

// This test's sole purpose is to check that implicit and explicit LOWER_BOUND defaulting work as
// expected.

import testlib.defaulting.LowerBoundQual.*;

class MyArrayList<MAL extends String> {}

class MyExplicitArray<MEA extends String> {}

public class LowerBoundDefaulting {

    // IMP1 is of type IMP1 [extends @LB_TOP super @LB_IMPLICIT]
    public <IMP1 extends String> void implicitsTypeVar() {

        // should fail because @LB_IMPLICIT is below @LB_TOP
        @LB_TOP MyArrayList<@LB_TOP ? extends @LB_TOP String> itLowerBoundIncompatible =
                // :: error: (assignment.type.incompatible)
                new MyArrayList<IMP1>();

        // :: error: (type.argument.type.incompatible)
        @LB_TOP MyArrayList<@LB_EXPLICIT ? extends @LB_TOP String> itLowerBoundStillIncompatible =
                // :: error: (assignment.type.incompatible)
                new MyArrayList<IMP1>();

        @LB_TOP MyArrayList<@LB_IMPLICIT ? extends @LB_TOP String> itLowerBoundCompatible =
                new MyArrayList<IMP1>();
    }

    public void implicitsWildcard(MyArrayList<?> myArrayList) {

        // should fail because @LB_IMPLICIT is below @LB_TOP
        // :: error: (assignment.type.incompatible)
        @LB_TOP MyArrayList<@LB_TOP ? extends @LB_TOP String> iwLowerBoundIncompatible = myArrayList;

        // :: error: (assignment.type.incompatible) :: error: (type.argument.type.incompatible)
        @LB_TOP MyArrayList<@LB_EXPLICIT ? extends @LB_TOP String> iwLowerBoundCompatible = myArrayList;

        @LB_TOP MyArrayList<@LB_IMPLICIT ? extends @LB_TOP String> iwLowerBoundStillCompatible =
                myArrayList;
    }

    public void implicitExtendBoundedWildcard(MyArrayList<? extends String> iebList) {

        // should fail because @LB_IMPLICIT is below @LB_TOP
        // :: error: (assignment.type.incompatible)
        @LB_TOP MyArrayList<@LB_TOP ? extends @LB_TOP String> iebLowerBoundIncompatible = iebList;

        // :: error: (assignment.type.incompatible) :: error: (type.argument.type.incompatible)
        @LB_TOP MyArrayList<@LB_EXPLICIT ? extends @LB_TOP String> iebLowerBoundStillIncompatible = iebList;

        @LB_TOP MyArrayList<@LB_IMPLICIT ? extends @LB_TOP String> iebLowerBoundCompatible = iebList;
    }

    // :: error: (type.argument.type.incompatible)
    public void explicitLowerBoundedWildcard(MyArrayList<? super String> elbList) {
        // should fail because @LB_EXPLICIT is below @LB_TOP
        // :: error: (assignment.type.incompatible)
        @LB_TOP MyArrayList<@LB_TOP ? super @LB_TOP String> iebLowerBoundIncompatible = elbList;

        // :: error: (type.argument.type.incompatible)
        @LB_TOP MyArrayList<@LB_TOP ? super @LB_EXPLICIT String> iebLowerBoundStillIncompatible = elbList;

        // :: error: (assignment.type.incompatible)
        @LB_TOP MyArrayList<@LB_TOP ? super @LB_IMPLICIT String> iebLowerBoundCompatible = elbList;
    }
}
