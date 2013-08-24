import checkers.javari.quals.*;

class ArrayTest {
    int @ReadOnly [] rmi;

    @Mutable Object aMM @Mutable[];
    @Mutable Object aMTm[];
    @ReadOnly Object aMRo @Mutable [];
    @Mutable Object aTmM[];
    Object aTmTm[];
    @ReadOnly Object aTmRo[];
    @Mutable Object aRoM @ReadOnly [];
    Object aRoTm @ReadOnly [];
    @ReadOnly Object aRoRo @ReadOnly [];

    void testPrimitiveArray() {
        int a;
        //:: error: (type.invalid)
        @ReadOnly int b;               // error
        int[] mmi;
        int @ReadOnly [] rmi;
        //:: error: (type.invalid)
        @ReadOnly int[] mri;            // error
        //:: error: (type.invalid)
        @ReadOnly int @ReadOnly [] rri;  // error
        int[][] mmmi;
        int[] @ReadOnly [] rmmi;
        int @ReadOnly [][] mrmi;
        int @ReadOnly [] @ReadOnly [] rrmi;
        //:: error: (type.invalid)
        @ReadOnly int[][] mmri;        // error
        //:: error: (type.invalid)
        @ReadOnly int[] @ReadOnly [] rmri; // error
        //:: error: (type.invalid)
        @ReadOnly int @ReadOnly [][] mrri;  // error
        //:: error: (type.invalid)
        @ReadOnly int @ReadOnly [] @ReadOnly [] rrri; // error

    }

    void testAM() {

        // aMM = ...
        aMM = aMM;
        aMM = aMTm;
        //:: error: (assignment.type.incompatible)
        aMM = aMRo;    // error
        aMM = aTmM;
        aMM = aTmTm;
        //:: error: (assignment.type.incompatible)
        aMM = aTmRo;   // error
        //:: error: (assignment.type.incompatible)
        aMM = aRoM;    // error
        //:: error: (assignment.type.incompatible)
        aMM = aRoTm;   // error
        //:: error: (assignment.type.incompatible)
        aMM = aRoRo;   // error

        // aMTm = ...
        aMTm = aMM;
        aMTm = aMTm;
        //:: error: (assignment.type.incompatible)
        aMTm = aMRo;   // error
        aMTm = aTmM;
        aMTm = aTmTm;
        //:: error: (assignment.type.incompatible)
        aMTm = aTmRo;  // error
        //:: error: (assignment.type.incompatible)
        aMTm = aRoM;   // error
        //:: error: (assignment.type.incompatible)
        aMTm = aRoTm;  // error
        //:: error: (assignment.type.incompatible)
        aMTm = aRoRo;  // error

        // aMRo = ...
        //TODOINVARR:: error: (assignment.type.incompatible)
        aMRo = aMM;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aMRo = aMTm;
        aMRo = aMRo;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aMRo = aTmM;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aMRo = aTmTm;
        aMRo = aTmRo;
        //:: error: (assignment.type.incompatible)
        aMRo = aRoM;   // error
        //:: error: (assignment.type.incompatible)
        aMRo = aRoTm;  // error
        //:: error: (assignment.type.incompatible)
        aMRo = aRoRo;  // error

    }

    void testATm() {

        // aTmM = ...
        aTmM = aMM;
        aTmM = aMTm;
        //:: error: (assignment.type.incompatible)
        aTmM = aMRo;   // error
        aTmM = aTmM;
        aTmM = aTmTm;
        //:: error: (assignment.type.incompatible)
        aTmM = aTmRo;  // error
        //:: error: (assignment.type.incompatible)
        aTmM = aRoM;   // error
        //:: error: (assignment.type.incompatible)
        aTmM = aRoTm;  // error
        //:: error: (assignment.type.incompatible)
        aTmM = aRoRo;  // error

        // aTmTm = ...
        aTmTm = aMM;
        aTmTm = aMTm;
        //:: error: (assignment.type.incompatible)
        aTmTm = aMRo;  // error
        aTmTm = aTmM;
        aTmTm = aTmTm;
        //:: error: (assignment.type.incompatible)
        aTmTm = aTmRo; // error
        //:: error: (assignment.type.incompatible)
        aTmTm = aRoM;  // error
        //:: error: (assignment.type.incompatible)
        aTmTm = aRoTm; // error
        //:: error: (assignment.type.incompatible)
        aTmTm = aRoRo; // error

        // aTmRo = ...
        //TODOINVARR:: error: (assignment.type.incompatible)
        aTmRo = aMM;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aTmRo = aMTm;
        aTmRo = aMRo;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aTmRo = aTmM;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aTmRo = aTmTm;
        aTmRo = aTmRo;
        //:: error: (assignment.type.incompatible)
        aTmRo = aRoM;  // error
        //:: error: (assignment.type.incompatible)
        aTmRo = aRoTm; // error
        //:: error: (assignment.type.incompatible)
        aTmRo = aRoRo; // error

    }

    void testARo() {

        // aRoM = ...
        aRoM = aMM;
        aRoM = aMTm;
        //:: error: (assignment.type.incompatible)
        aRoM = aMRo;   // error
        aRoM = aTmM;
        aRoM = aTmTm;
        //:: error: (assignment.type.incompatible)
        aRoM = aTmRo;  // error
        aRoM = aRoM;
        aRoM = aRoTm;
        //:: error: (assignment.type.incompatible)
        aRoM = aRoRo;  // error

        // aRoTm = ...
        aRoTm = aMM;
        aRoTm = aMTm;
        //:: error: (assignment.type.incompatible)
        aRoTm = aMRo;  // error
        aRoTm = aTmM;
        aRoTm = aTmTm;
        //:: error: (assignment.type.incompatible)
        aRoTm = aTmRo; // error
        aRoTm = aRoM;
        aRoTm = aRoTm;
        //:: error: (assignment.type.incompatible)
        aRoTm = aRoRo; // error

        // aRoRo = ...
        //TODOINVARR:: error: (assignment.type.incompatible)
        aRoRo = aMM;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aRoRo = aMTm;
        aRoRo = aMRo;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aRoRo = aTmM;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aRoRo = aTmTm;
        aRoRo = aTmRo;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aRoRo = aRoM;
        //TODOINVARR:: error: (assignment.type.incompatible)
        aRoRo = aRoTm;
        aRoRo = aRoRo;

    }

    void testAMZero() {

        // aMM = ...
        aMM[0] = aMM[0];
        aMM[0] = aMTm[0];
        //:: error: (assignment.type.incompatible)
        aMM[0] = aMRo[0];    // error
        aMM[0] = aTmM[0];
        aMM[0] = aTmTm[0];
        //:: error: (assignment.type.incompatible)
        aMM[0] = aTmRo[0];   // error
        aMM[0] = aRoM[0];
        aMM[0] = aRoTm[0];
        //:: error: (assignment.type.incompatible)
        aMM[0] = aRoRo[0];   // error

        // aMTm = ...
        aMTm[0] = aMM[0];
        aMTm[0] = aMTm[0];
        //:: error: (assignment.type.incompatible)
        aMTm[0] = aMRo[0];   // error
        aMTm[0] = aTmM[0];
        aMTm[0] = aTmTm[0];
        //:: error: (assignment.type.incompatible)
        aMTm[0] = aTmRo[0];  // error
        aMTm[0] = aRoM[0];
        aMTm[0] = aRoTm[0];
        //:: error: (assignment.type.incompatible)
        aMTm[0] = aRoRo[0];  // error

        // aMRo = ...
        aMRo[0] = aMM[0];
        aMRo[0] = aMTm[0];
        aMRo[0] = aMRo[0];
        aMRo[0] = aTmM[0];
        aMRo[0] = aTmTm[0];
        aMRo[0] = aTmRo[0];
        aMRo[0] = aRoM[0];
        aMRo[0] = aRoTm[0];
        aMRo[0] = aRoRo[0];

    }

    void testATmZero() {

        // aTmM = ...
        aTmM[0] = aMM[0];
        aTmM[0] = aMTm[0];
        //:: error: (assignment.type.incompatible)
        aTmM[0] = aMRo[0];   // error
        aTmM[0] = aTmM[0];
        aTmM[0] = aTmTm[0];
        //:: error: (assignment.type.incompatible)
        aTmM[0] = aTmRo[0];  // error
        aTmM[0] = aRoM[0];
        aTmM[0] = aRoTm[0];
        //:: error: (assignment.type.incompatible)
        aTmM[0] = aRoRo[0];  // error

        // aTmTm = ...
        aTmTm[0] = aMM[0];
        aTmTm[0] = aMTm[0];
        //:: error: (assignment.type.incompatible)
        aTmTm[0] = aMRo[0];  // error
        aTmTm[0] = aTmM[0];
        aTmTm[0] = aTmTm[0];
        //:: error: (assignment.type.incompatible)
        aTmTm[0] = aTmRo[0]; // error
        aTmTm[0] = aRoM[0];
        aTmTm[0] = aRoTm[0];
        //:: error: (assignment.type.incompatible)
        aTmTm[0] = aRoRo[0]; // error

        // aTmRo = ...
        aTmRo[0] = aMM[0];
        aTmRo[0] = aMTm[0];
        aTmRo[0] = aMRo[0];
        aTmRo[0] = aTmM[0];
        aTmRo[0] = aTmTm[0];
        aTmRo[0] = aTmRo[0];
        aTmRo[0] = aRoM[0];
        aTmRo[0] = aRoTm[0];
        aTmRo[0] = aRoRo[0];

    }

    void testARoZero() {

        // aRoM = ...
        //:: error: (ro.element)
        aRoM[0] = aMM[0];
        //:: error: (ro.element)
        aRoM[0] = aMTm[0];
        //:: error: (assignment.type.incompatible) :: error: (ro.element)
        aRoM[0] = aMRo[0];   // error
        //:: error: (ro.element)
        aRoM[0] = aTmM[0];
        //:: error: (ro.element)
        aRoM[0] = aTmTm[0];
        //:: error: (assignment.type.incompatible) :: error: (ro.element)
        aRoM[0] = aTmRo[0];  // error
        //:: error: (ro.element)
        aRoM[0] = aRoM[0];
        //:: error: (ro.element)
        aRoM[0] = aRoTm[0];
        //:: error: (assignment.type.incompatible) :: error: (ro.element)
        aRoM[0] = aRoRo[0];  // error

        // aRoTm = ...
        //:: error: (ro.element)
        aRoTm[0] = aMM[0];
        //:: error: (ro.element)
        aRoTm[0] = aMTm[0];
        //:: error: (assignment.type.incompatible) :: error: (ro.element)
        aRoTm[0] = aMRo[0];  // error
        //:: error: (ro.element)
        aRoTm[0] = aTmM[0];
        //:: error: (ro.element)
        aRoTm[0] = aTmTm[0];
        //:: error: (assignment.type.incompatible) :: error: (ro.element)
        aRoTm[0] = aTmRo[0]; // error
        //:: error: (ro.element)
        aRoTm[0] = aRoM[0];
        //:: error: (ro.element)
        aRoTm[0] = aRoTm[0];
        //:: error: (assignment.type.incompatible) :: error: (ro.element)
        aRoTm[0] = aRoRo[0]; // error

        // aRoRo = ...
        //:: error: (ro.element)
        aRoRo[0] = aMM[0];
        //:: error: (ro.element)
        aRoRo[0] = aMTm[0];
        //:: error: (ro.element)
        aRoRo[0] = aMRo[0];
        //:: error: (ro.element)
        aRoRo[0] = aTmM[0];
        //:: error: (ro.element)
        aRoRo[0] = aTmTm[0];
        //:: error: (ro.element)
        aRoRo[0] = aTmRo[0];
        //:: error: (ro.element)
        aRoRo[0] = aRoM[0];
        //:: error: (ro.element)
        aRoRo[0] = aRoTm[0];
        //:: error: (ro.element)
        aRoRo[0] = aRoRo[0];

    }

    public void testARoAssignability(double @ReadOnly [] d) {
        //:: error: (ro.element)
        rmi[0] = 0;
        //:: error: (ro.element)
        aRoTm[0] = new Object();
    }

}
