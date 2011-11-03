import checkers.nullness.quals.*;
public class Boxing {
    void withinOperation() {
        Integer i1 = 3;
        int i1u = i1 + 2;         // valid
        Integer i2 = null;
        //:: error: (unboxing.of.nullable)
        int i2u = i2 + 2;         // invalid
        Integer i3 = i1;
        i3.toString();
    }

    void withinAssignment() {
        Integer i1 = 5;
        int i1u = i1;
        Integer i2 = null;
        //:: error: (assignment.type.incompatible)
        int i2u = i2;       // invalid
    }

    void validWithinUnary() {
        // within blocks to stop flow
        Integer i1 = 1, i2 = 1, i3 = 1, i4 = 1;
        ++i1;
        i2++;
    }

    void invalidWithinUnary() {
        // within blocks to stop flow
        Integer i1 = null, i2 = null, i3 = null, i4 = null;
        //:: error: (unboxing.of.nullable)
        ++i1;       // invalid
        //:: error: (unboxing.of.nullable)
        i2++;       // invalid
    }

    void validCompoundAssignmentsAsVariable() {
        @NonNull Integer i = 0; // nonnull is needed because flow is buggy
        i += 1;
        i -= 1;
        @NonNull Boolean b = true;
        b &= true;
    }

    void invalidCompoundAssignmentsAsVariable() {
        Integer i = null;
        //:: error: (unboxing.of.nullable)
        i += 1;         // invalid
        Boolean b = null;
        //:: error: (unboxing.of.nullable)
        b &= true;      // invalid
    }

    void invalidCompoundAssignmentAsValue() {
        @NonNull Integer var = 3;
        Integer val = null;
        //:: error: (unboxing.of.nullable)
        var += val;
        Boolean b1 = null;
        boolean b2 = true;
        //:: error: (unboxing.of.nullable)
        b2 &= b1;     // invalid
    }

    void randomValidStringOperations() {
        String s = null;
        s += null;
    }

    void equalityTest() {
        Integer bN = null;
        Integer b1 = 1;
        int u1 = 1;
        System.out.println(bN == bN); // valid
        System.out.println(bN == b1); // valid
        System.out.println(bN != bN); // valid
        System.out.println(bN != b1); // valid

        System.out.println(u1 == b1);
        System.out.println(u1 != b1);
        System.out.println(u1 == u1);
        System.out.println(u1 != u1);

        //:: error: (unboxing.of.nullable)
        System.out.println(bN == u1); // invalid
        //:: error: (unboxing.of.nullable)
        System.out.println(bN != u1); // invalid
    }

    void addition() {
        Integer bN = null;
        Integer b1 = 1;
        int u1 = 1;
        //:: error: (unboxing.of.nullable)
        System.out.println(bN + bN); // invalid
        //:: error: (unboxing.of.nullable)
        System.out.println(bN + b1); // invalid

        System.out.println(u1 + b1);
        System.out.println(u1 + u1);

        //:: error: (unboxing.of.nullable)
        System.out.println(bN + u1); // invalid
    }

    void visitCast() {
        Integer bN = null;
        Integer b1 = 1;
        int u1 = 1;

        println(bN);
        //:: error: (unboxing.of.nullable)
        println((int)bN); // invalid

        println(b1);
        println((int)b1);

        println(u1);
        println((int)u1);
    }

    void println(@Nullable Object o) { }

    void testObjectString() {
        Object o = null;
        o += "m";
    }

    void testCharString() {
        CharSequence cs = null;
        cs += "m";
    }
}
