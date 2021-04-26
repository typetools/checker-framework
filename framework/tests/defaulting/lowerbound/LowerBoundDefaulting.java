package defaulting.lowerbound;

// This test's sole purpose is to check that implicit and explicit LOWER_BOUND defaulting work as
// expected.

import org.checkerframework.framework.testchecker.defaulting.LowerBoundQual.*;

class MyArrayList<MAL extends String> {}

class MyExplicitArray<MEA extends String> {}

public class LowerBoundDefaulting {

  // IMP1 is of type IMP1 [extends @LbTop super @LbImplicit]
  public <IMP1 extends String> void implicitsTypeVar() {

    // should fail because @LbImplicit is below @LbTop
    @LbTop MyArrayList<@LbTop ? extends @LbTop String> itLowerBoundIncompatible =
        // :: error: (assignment.type.incompatible)
        new MyArrayList<IMP1>();

    @LbTop MyArrayList<@LbExplicit ? extends @LbTop String> itLowerBoundStillIncompatible =
        // :: error: (assignment.type.incompatible)
        new MyArrayList<IMP1>();

    @LbTop MyArrayList<@LbImplicit ? extends @LbTop String> itLowerBoundCompatible =
        new MyArrayList<IMP1>();
  }

  public void implicitsWildcard(MyArrayList<?> myArrayList) {

    // should fail because @LbImplicit is below @LbTop
    // :: error: (assignment.type.incompatible)
    @LbTop MyArrayList<@LbTop ? extends @LbTop String> iwLowerBoundIncompatible = myArrayList;

    // :: error: (assignment.type.incompatible)
    @LbTop MyArrayList<@LbExplicit ? extends @LbTop String> iwLowerBoundCompatible = myArrayList;

    @LbTop MyArrayList<@LbImplicit ? extends @LbTop String> iwLowerBoundStillCompatible = myArrayList;
  }

  public void implicitExtendBoundedWildcard(MyArrayList<? extends String> iebList) {

    // should fail because @LbImplicit is below @LbTop
    // :: error: (assignment.type.incompatible)
    @LbTop MyArrayList<@LbTop ? extends @LbTop String> iebLowerBoundIncompatible = iebList;

    // :: error: (assignment.type.incompatible)
    @LbTop MyArrayList<@LbExplicit ? extends @LbTop String> iebLowerBoundStillIncompatible = iebList;

    @LbTop MyArrayList<@LbImplicit ? extends @LbTop String> iebLowerBoundCompatible = iebList;
  }

  // :: error: (type.argument.type.incompatible)
  public void explicitLowerBoundedWildcard(MyArrayList<? super String> elbList) {
    // should fail because @LbExplicit is below @LbTop
    // :: error: (assignment.type.incompatible)
    @LbTop MyArrayList<@LbTop ? super @LbTop String> iebLowerBoundIncompatible = elbList;

    // :: error: (type.argument.type.incompatible)
    @LbTop MyArrayList<@LbTop ? super @LbExplicit String> iebLowerBoundStillIncompatible = elbList;

    // :: error: (assignment.type.incompatible)
    @LbTop MyArrayList<@LbTop ? super @LbImplicit String> iebLowerBoundCompatible = elbList;
  }
}
