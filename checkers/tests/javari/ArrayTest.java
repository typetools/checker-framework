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
        //:: (primitive.ro)
        @ReadOnly int b;               // error
        int[] mmi;
        int @ReadOnly [] rmi;
        //:: (primitive.ro)
        @ReadOnly int[] mri;            // error
        //:: (primitive.ro)
        @ReadOnly int @ReadOnly [] rri;  // error
        int[][] mmmi;
        int[] @ReadOnly [] rmmi;
        int @ReadOnly [][] mrmi;
        int @ReadOnly [] @ReadOnly [] rrmi;
        //:: (primitive.ro)
        @ReadOnly int[][] mmri;        // error
        //:: (primitive.ro)
        @ReadOnly int[] @ReadOnly [] rmri; // error
        //:: (primitive.ro)
        @ReadOnly int @ReadOnly [][] mrri;  // error
        //:: (primitive.ro)
        @ReadOnly int @ReadOnly [] @ReadOnly [] rrri; // error

    }

    void testAM() {

        // aMM = ...
        aMM = aMM;
        aMM = aMTm;
        //:: (type.incompatible)
        aMM = aMRo;    // error
        aMM = aTmM;
        aMM = aTmTm;
        //:: (type.incompatible)
        aMM = aTmRo;   // error
        //:: (type.incompatible)
        aMM = aRoM;    // error
        //:: (type.incompatible)
        aMM = aRoTm;   // error
        //:: (type.incompatible)
        aMM = aRoRo;   // error

        // aMTm = ...
        aMTm = aMM;
        aMTm = aMTm;
        //:: (type.incompatible)
        aMTm = aMRo;   // error
        aMTm = aTmM;
        aMTm = aTmTm;
        //:: (type.incompatible)
        aMTm = aTmRo;  // error
        //:: (type.incompatible)
        aMTm = aRoM;   // error
        //:: (type.incompatible)
        aMTm = aRoTm;  // error
        //:: (type.incompatible)
        aMTm = aRoRo;  // error

        // aMRo = ...
        aMRo = aMM;
        aMRo = aMTm;
        aMRo = aMRo;
        aMRo = aTmM;
        aMRo = aTmTm;
        aMRo = aTmRo;
        //:: (type.incompatible)
        aMRo = aRoM;   // error
        //:: (type.incompatible)
        aMRo = aRoTm;  // error
        //:: (type.incompatible)
        aMRo = aRoRo;  // error

    }

    void testATm() {

        // aTmM = ...
        aTmM = aMM;
        aTmM = aMTm;
        //:: (type.incompatible)
        aTmM = aMRo;   // error
        aTmM = aTmM;
        aTmM = aTmTm;
        //:: (type.incompatible)
        aTmM = aTmRo;  // error
        //:: (type.incompatible)
        aTmM = aRoM;   // error
        //:: (type.incompatible)
        aTmM = aRoTm;  // error
        //:: (type.incompatible)
        aTmM = aRoRo;  // error

        // aTmTm = ...
        aTmTm = aMM;
        aTmTm = aMTm;
        //:: (type.incompatible)
        aTmTm = aMRo;  // error
        aTmTm = aTmM;
        aTmTm = aTmTm;
        //:: (type.incompatible)
        aTmTm = aTmRo; // error
        //:: (type.incompatible)
        aTmTm = aRoM;  // error
        //:: (type.incompatible)
        aTmTm = aRoTm; // error
        //:: (type.incompatible)
        aTmTm = aRoRo; // error

        // aTmRo = ...
        aTmRo = aMM;
        aTmRo = aMTm;
        aTmRo = aMRo;
        aTmRo = aTmM;
        aTmRo = aTmTm;
        aTmRo = aTmRo;
        //:: (type.incompatible)
        aTmRo = aRoM;  // error
        //:: (type.incompatible)
        aTmRo = aRoTm; // error
        //:: (type.incompatible)
        aTmRo = aRoRo; // error

    }

    void testARo() {

        // aRoM = ...
        aRoM = aMM;
        aRoM = aMTm;
        //:: (type.incompatible)
        aRoM = aMRo;   // error
        aRoM = aTmM;
        aRoM = aTmTm;
        //:: (type.incompatible)
        aRoM = aTmRo;  // error
        aRoM = aRoM;
        aRoM = aRoTm;
        //:: (type.incompatible)
        aRoM = aRoRo;  // error

        // aRoTm = ...
        aRoTm = aMM;
        aRoTm = aMTm;
        //:: (type.incompatible)
        aRoTm = aMRo;  // error
        aRoTm = aTmM;
        aRoTm = aTmTm;
        //:: (type.incompatible)
        aRoTm = aTmRo; // error
        aRoTm = aRoM;
        aRoTm = aRoTm;
        //:: (type.incompatible)
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
        //:: (type.incompatible)
        aMM[0] = aMRo[0];    // error
        aMM[0] = aTmM[0];
        aMM[0] = aTmTm[0];
        //:: (type.incompatible)
        aMM[0] = aTmRo[0];   // error
        aMM[0] = aRoM[0];
        aMM[0] = aRoTm[0];
        //:: (type.incompatible)
        aMM[0] = aRoRo[0];   // error

        // aMTm = ...
        aMTm[0] = aMM[0];
        aMTm[0] = aMTm[0];
        //:: (type.incompatible)
        aMTm[0] = aMRo[0];   // error
        aMTm[0] = aTmM[0];
        aMTm[0] = aTmTm[0];
        //:: (type.incompatible)
        aMTm[0] = aTmRo[0];  // error
        aMTm[0] = aRoM[0];
        aMTm[0] = aRoTm[0];
        //:: (type.incompatible)
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
        //:: (type.incompatible)
        aTmM[0] = aMRo[0];   // error
        aTmM[0] = aTmM[0];
        aTmM[0] = aTmTm[0];
        //:: (type.incompatible)
        aTmM[0] = aTmRo[0];  // error
        aTmM[0] = aRoM[0];
        aTmM[0] = aRoTm[0];
        //:: (type.incompatible)
        aTmM[0] = aRoRo[0];  // error

        // aTmTm = ...
        aTmTm[0] = aMM[0];
        aTmTm[0] = aMTm[0];
        //:: (type.incompatible)
        aTmTm[0] = aMRo[0];  // error
        aTmTm[0] = aTmM[0];
        aTmTm[0] = aTmTm[0];
        //:: (type.incompatible)
        aTmTm[0] = aTmRo[0]; // error
        aTmTm[0] = aRoM[0];
        aTmTm[0] = aRoTm[0];
        //:: (type.incompatible)
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
        //:: (type.incompatible)
        aRoM[0] = aMRo[0];   // error
        aRoM[0] = aTmM[0];
        aRoM[0] = aTmTm[0];
        //:: (type.incompatible)
        aRoM[0] = aTmRo[0];  // error
        aRoM[0] = aRoM[0];
        aRoM[0] = aRoTm[0];
        //:: (type.incompatible)
        aRoM[0] = aRoRo[0];  // error

        // aRoTm = ...
        aRoTm[0] = aMM[0];
        aRoTm[0] = aMTm[0];
        //:: (type.incompatible)
        aRoTm[0] = aMRo[0];  // error
        aRoTm[0] = aTmM[0];
        aRoTm[0] = aTmTm[0];
        //:: (type.incompatible)
        aRoTm[0] = aTmRo[0]; // error
        aRoTm[0] = aRoM[0];
        aRoTm[0] = aRoTm[0];
        //:: (type.incompatible)
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
