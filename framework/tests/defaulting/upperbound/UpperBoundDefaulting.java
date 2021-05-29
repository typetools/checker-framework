package defaulting.upperbound;

// This test's sole purpose is to check that implicit and explicit LOWER_BOUND defaulting work as
// expected.

import org.checkerframework.framework.testchecker.defaulting.UpperBoundQual.*;

class MyArrayList<MAL extends String> {}

class MyExplicitArray<MEA extends String> {}

public class UpperBoundDefaulting {

  public <UAL extends String> void explicitUpperBoundTypeVar() {
    MyArrayList<@UbBottom ? extends @UbBottom Object> eubBottomToBottom =
        // :: error: (assignment)
        new MyArrayList<UAL>();

    MyArrayList<@UbBottom ? extends @UbExplicit Object> eubExplicitToBottom =
        new MyArrayList<UAL>();

    // :: error: (type.argument)
    MyArrayList<@UbBottom ? extends @UbImplicit Object> eubImplicitToBottom =
        // :: error: (assignment)
        new MyArrayList<UAL>();
  }

  public void implicitsWildcard(MyArrayList<?> myArrayList) {

    // should fail because @LbImplicit is below @UbTop
    // :: error: (type.argument)
    @UbTop MyArrayList<@UbBottom ? extends @UbTop String> iwLowerBoundIncompatible = myArrayList;

    @UbTop MyArrayList<@UbBottom ? extends @UbExplicit String> iwLowerBoundCompatible = myArrayList;

    // :: error: (type.argument)
    @UbTop MyArrayList<@UbBottom ? extends @UbImplicit String> iwLowerBoundStillCompatible =
        // :: error: (assignment)
        myArrayList;
  }

  public void implicitExtendBoundedWildcard(MyArrayList<? extends String> iebList) {

    // should fail because @LbImplicit is below @UbTop
    // :: error: (type.argument)
    @UbTop MyArrayList<@UbBottom ? extends @UbTop String> iebLowerBoundIncompatible = iebList;

    MyArrayList<@UbBottom ? extends @UbExplicit Object> eubExplicitToBottom = iebList;

    // :: error: (assignment) :: error: (type.argument)
    MyArrayList<@UbBottom ? extends @UbImplicit Object> eubImplicitToBottom = iebList;
  }

  public void explicitLowerBoundedWildcard(MyArrayList<? super String> elbList) {
    // should fail because @UbExplicit is below UbTop
    // :: error: (type.argument)
    @UbTop MyArrayList<@UbTop ? super @UbBottom String> iebLowerBoundIncompatible = elbList;

    // :: error: (type.argument)
    @UbTop MyArrayList<@UbImplicit ? super @UbBottom String> iebLowerBoundStillIncompatible =
        // :: error: (assignment)
        elbList;

    @UbTop MyArrayList<@UbExplicit ? super @UbBottom String> iebLowerBoundCompatible = elbList;
  }
}
