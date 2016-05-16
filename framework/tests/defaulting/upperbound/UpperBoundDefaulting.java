package defaulting.upperbound;

// this tests sole purpose is to check that implicit and explicit LOWER_BOUND defaulting work as expected

import tests.defaulting.UpperBoundQual.*;

class MyArrayList<MAL extends String>{}
class MyExplicitArray<MEA extends String>{}

public class UpperBoundDefaulting {

    public <UAL extends String> void explicitUpperBoundTypeVar() {
        //:: error: (assignment.type.incompatible)
        MyArrayList<@UB_BOTTOM ? extends @UB_BOTTOM Object> eubBottomToBottom =  new MyArrayList<UAL>();

        MyArrayList<@UB_BOTTOM ? extends @UB_EXPLICIT Object> eubExplicitToBottom =  new MyArrayList<UAL>();

        //:: error: (assignment.type.incompatible) :: error: (type.argument.type.incompatible)
        MyArrayList<@UB_BOTTOM ? extends @UB_IMPLICIT Object> eubImplicitToBottom =  new MyArrayList<UAL>();
    }

    public void implicitsWildcard(MyArrayList<?> myArrayList) {

        // should fail because @LB_IMPLICIT is below @UB_TOP
        //:: error: (type.argument.type.incompatible)
        @UB_TOP MyArrayList<@UB_BOTTOM ? extends @UB_TOP String> iwLowerBoundIncompatible = myArrayList;

        @UB_TOP MyArrayList<@UB_BOTTOM ? extends @UB_EXPLICIT String> iwLowerBoundCompatible = myArrayList;

        //:: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
        @UB_TOP MyArrayList<@UB_BOTTOM ? extends @UB_IMPLICIT String> iwLowerBoundStillCompatible = myArrayList;

    }

    public void implicitExtendBoundedWildcard(MyArrayList<? extends String> iebList) {

        // should fail because @LB_IMPLICIT is below @UB_TOP
        //:: error: (type.argument.type.incompatible)
        @UB_TOP MyArrayList<@UB_BOTTOM ? extends @UB_TOP String> iebLowerBoundIncompatible = iebList;

        MyArrayList<@UB_BOTTOM ? extends @UB_EXPLICIT Object> eubExplicitToBottom =  iebList;

        //:: error: (assignment.type.incompatible) :: error: (type.argument.type.incompatible)
        MyArrayList<@UB_BOTTOM ? extends @UB_IMPLICIT Object> eubImplicitToBottom =  iebList;
    }

    public void explicitLowerBoundedWildcard(MyArrayList<? super String> elbList) {
        // should fail because @UB_EXPLICIT is below UB_TOP
        //:: error: (type.argument.type.incompatible)
        @UB_TOP MyArrayList<@UB_TOP ? super @UB_BOTTOM String> iebLowerBoundIncompatible = elbList;

        //:: error: (assignment.type.incompatible) :: error: (type.argument.type.incompatible)
        @UB_TOP MyArrayList<@UB_IMPLICIT ? super @UB_BOTTOM String> iebLowerBoundStillIncompatible = elbList;

        @UB_TOP MyArrayList<@UB_EXPLICIT ? super @UB_BOTTOM String> iebLowerBoundCompatible = elbList;

    }
}
