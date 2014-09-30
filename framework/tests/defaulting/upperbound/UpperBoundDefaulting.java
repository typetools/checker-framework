package defaulting.upperbound;

//this tests sole purpose is to check that implicit and explicit LOWER_BOUND defaulting work as expected

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
}
