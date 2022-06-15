package defaulting.lowerbound;

// This test's sole purpose is to check that implicit and explicit LOWER_BOUND defaulting work as
// expected.

import org.checkerframework.framework.testchecker.defaulting.LowerBoundQual.*;

class MyArrayList<MAL extends CharSequence> {}

class MyExplicitArray<MEA extends CharSequence> {}

public class LowerBoundDefaulting {

  // IMP1 is of type IMP1 [extends @LbTop super @LbImplicit]
  public <IMP1 extends String> void implicitsTypeVar() {

    // should fail because @LbImplicit is below @LbTop
    @LbTop MyArrayList<@LbTop ? extends @LbTop String> itLowerBoundIncompatible =
        // :: error: (assignment)
        new MyArrayList<IMP1>();

    @LbTop MyArrayList<@LbExplicit ? extends @LbTop String> itLowerBoundStillIncompatible =
        // :: error: (assignment)
        new MyArrayList<IMP1>();

    @LbTop MyArrayList<@LbImplicit ? extends @LbTop String> itLowerBoundCompatible =
        new MyArrayList<IMP1>();
  }

  public void implicitsWildcard(MyArrayList<?> myArrayList) {

    // should fail because @LbImplicit is below @LbTop
    // :: error: (assignment)
    @LbTop MyArrayList<@LbTop ? extends @LbTop CharSequence> iwLowerBoundIncompatible = myArrayList;

    // :: error: (assignment)
    @LbTop MyArrayList<@LbExplicit ? extends @LbTop CharSequence> iwLowerBoundCompatible = myArrayList;

    @LbTop MyArrayList<@LbImplicit ? extends @LbTop CharSequence> iwLowerBoundStillCompatible =
        myArrayList;
  }

  public void implicitExtendBoundedWildcard(MyArrayList<? extends String> iebList) {

    // should fail because @LbImplicit is below @LbTop
    // :: error: (assignment)
    @LbTop MyArrayList<@LbTop ? extends @LbTop String> iebLowerBoundIncompatible = iebList;

    // :: error: (assignment)
    @LbTop MyArrayList<@LbExplicit ? extends @LbTop String> iebLowerBoundStillIncompatible = iebList;

    @LbTop MyArrayList<@LbImplicit ? extends @LbTop String> iebLowerBoundCompatible = iebList;
  }

  // MyArrayList<@LbTop MAL extends @LbTop CharSequence>
  // elbLsit is MyArrayList<@LbTop ? super @LbExplicit String>
  // capture is MyArrayList<cap#1 @LbTop ? super @LbTop String>
  // The super bound is the LUB of @LbTop and @LbExplicit.
  public void explicitLowerBoundedWildcard(MyArrayList<? super String> elbList) {
    // :: error: (assignment)
    @LbTop MyArrayList<@LbBottom ? super @LbBottom String> iebLowerBoundIncompatible = elbList;

    @LbTop MyArrayList<@LbTop ? super @LbExplicit String> iebLowerBoundStillIncompatible = elbList;

    @LbTop MyArrayList<@LbTop ? super @LbImplicit String> iebLowerBoundCompatible = elbList;
  }
}
