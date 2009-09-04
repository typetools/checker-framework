import checkers.javari.quals.*;

class ArrayTest {         int @ReadOnly [] rmi;

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
        @ReadOnly int b;               // error
        int[] mmi;
        int @ReadOnly [] rmi;
        @ReadOnly int[] mri;            // error
        @ReadOnly int @ReadOnly [] rri;  // error
        int[][] mmmi;
        int[] @ReadOnly [] rmmi;
        int @ReadOnly [][] mrmi;
        int @ReadOnly [] @ReadOnly [] rrmi;
        @ReadOnly int[][] mmri;        // error
        @ReadOnly int[] @ReadOnly [] rmri; // error
        @ReadOnly int @ReadOnly [][] mrri;  // error
        @ReadOnly int @ReadOnly [] @ReadOnly [] rrri; // error

    }

    void testAM() {

        // aMM = ...
        aMM = aMM;
        aMM = aMTm;
        aMM = aMRo;    // error
        aMM = aTmM;
        aMM = aTmTm;
        aMM = aTmRo;   // error
        aMM = aRoM;    // error
        aMM = aRoTm;   // error
        aMM = aRoRo;   // error

        // aMTm = ...
        aMTm = aMM;
        aMTm = aMTm;
        aMTm = aMRo;   // error
        aMTm = aTmM;
        aMTm = aTmTm;
        aMTm = aTmRo;  // error
        aMTm = aRoM;   // error
        aMTm = aRoTm;  // error
        aMTm = aRoRo;  // error

        // aMRo = ...
        aMRo = aMM;
        aMRo = aMTm;
        aMRo = aMRo;
        aMRo = aTmM;
        aMRo = aTmTm;
        aMRo = aTmRo;
        aMRo = aRoM;   // error
        aMRo = aRoTm;  // error
        aMRo = aRoRo;  // error

    }

    void testATm() {

        // aTmM = ...
        aTmM = aMM;
        aTmM = aMTm;
        aTmM = aMRo;   // error
        aTmM = aTmM;
        aTmM = aTmTm;
        aTmM = aTmRo;  // error
        aTmM = aRoM;   // error
        aTmM = aRoTm;  // error
        aTmM = aRoRo;  // error

        // aTmTm = ...
        aTmTm = aMM;
        aTmTm = aMTm;
        aTmTm = aMRo;  // error
        aTmTm = aTmM;
        aTmTm = aTmTm;
        aTmTm = aTmRo; // error
        aTmTm = aRoM;  // error
        aTmTm = aRoTm; // error
        aTmTm = aRoRo; // error

        // aTmRo = ...
        aTmRo = aMM;
        aTmRo = aMTm;
        aTmRo = aMRo;
        aTmRo = aTmM;
        aTmRo = aTmTm;
        aTmRo = aTmRo;
        aTmRo = aRoM;  // error
        aTmRo = aRoTm; // error
        aTmRo = aRoRo; // error

    }

    void testARo() {

        // aRoM = ...
        aRoM = aMM;
        aRoM = aMTm;
        aRoM = aMRo;   // error
        aRoM = aTmM;
        aRoM = aTmTm;
        aRoM = aTmRo;  // error
        aRoM = aRoM;
        aRoM = aRoTm;
        aRoM = aRoRo;  // error

        // aRoTm = ...
        aRoTm = aMM;
        aRoTm = aMTm;
        aRoTm = aMRo;  // error
        aRoTm = aTmM;
        aRoTm = aTmTm;
        aRoTm = aTmRo; // error
        aRoTm = aRoM;
        aRoTm = aRoTm;
        aRoTm = aRoRo; // error

        // aRoRo = ...
        aRoRo = aMM;
        aRoRo = aMTm;
        aRoRo = aMRo;
        aRoRo = aTmM;
        aRoRo = aTmTm;
        aRoRo = aTmRo;
        aRoRo = aRoM;
        aRoRo = aRoTm;
        aRoRo = aRoRo;

    }

    void testAMZero() {

        // aMM = ...
        aMM[0] = aMM[0];
        aMM[0] = aMTm[0];
        aMM[0] = aMRo[0];    // error
        aMM[0] = aTmM[0];
        aMM[0] = aTmTm[0];
        aMM[0] = aTmRo[0];   // error
        aMM[0] = aRoM[0];
        aMM[0] = aRoTm[0];
        aMM[0] = aRoRo[0];   // error

        // aMTm = ...
        aMTm[0] = aMM[0];
        aMTm[0] = aMTm[0];
        aMTm[0] = aMRo[0];   // error
        aMTm[0] = aTmM[0];
        aMTm[0] = aTmTm[0];
        aMTm[0] = aTmRo[0];  // error
        aMTm[0] = aRoM[0];
        aMTm[0] = aRoTm[0];
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
        aTmM[0] = aMRo[0];   // error
        aTmM[0] = aTmM[0];
        aTmM[0] = aTmTm[0];
        aTmM[0] = aTmRo[0];  // error
        aTmM[0] = aRoM[0];
        aTmM[0] = aRoTm[0];
        aTmM[0] = aRoRo[0];  // error

        // aTmTm = ...
        aTmTm[0] = aMM[0];
        aTmTm[0] = aMTm[0];
        aTmTm[0] = aMRo[0];  // error
        aTmTm[0] = aTmM[0];
        aTmTm[0] = aTmTm[0];
        aTmTm[0] = aTmRo[0]; // error
        aTmTm[0] = aRoM[0];
        aTmTm[0] = aRoTm[0];
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
        aRoM[0] = aMM[0];
        aRoM[0] = aMTm[0];
        aRoM[0] = aMRo[0];   // error
        aRoM[0] = aTmM[0];
        aRoM[0] = aTmTm[0];
        aRoM[0] = aTmRo[0];  // error
        aRoM[0] = aRoM[0];
        aRoM[0] = aRoTm[0];
        aRoM[0] = aRoRo[0];  // error

        // aRoTm = ...
        aRoTm[0] = aMM[0];
        aRoTm[0] = aMTm[0];
        aRoTm[0] = aMRo[0];  // error
        aRoTm[0] = aTmM[0];
        aRoTm[0] = aTmTm[0];
        aRoTm[0] = aTmRo[0]; // error
        aRoTm[0] = aRoM[0];
        aRoTm[0] = aRoTm[0];
        aRoTm[0] = aRoRo[0]; // error

        // aRoRo = ...
        aRoRo[0] = aMM[0];
        aRoRo[0] = aMTm[0];
        aRoRo[0] = aMRo[0];
        aRoRo[0] = aTmM[0];
        aRoRo[0] = aTmTm[0];
        aRoRo[0] = aTmRo[0];
        aRoRo[0] = aRoM[0];
        aRoRo[0] = aRoTm[0];
        aRoRo[0] = aRoRo[0];

    }
}
