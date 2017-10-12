import polyall.quals.*;

class CompoundStringAssignment {
    @H1S1 @H2S1 String getSib1() {
        return null;
    }

    void test1() {
        String local = null;
        // There was a bug in data flow where
        // the type of local was @H1Bot @H2Bot after this
        // StringConcatenateAssignmentNode,
        // but only if the RHS was a method call.
        local += getSib1();

        // :: error: (assignment.type.incompatible)
        @H1Bot @H2Bot String isBot = local;
        @H1S1 @H2S1 String isSib1 = local;
    }

    @H1Top @H2Top String top;

    void test2() {
        String local2 = top;
        local2 += getSib1();

        // :: error: (assignment.type.incompatible)
        @H1Bot @H2Bot String isBot2 = local2;
        // :: error: (assignment.type.incompatible)
        @H1S1 @H2S1 String isSib12 = local2;
    }

    @H1S1 @H2S1 String sib1;

    void test3() {
        String local3 = null;
        local3 += sib1;

        // :: error: (assignment.type.incompatible)
        @H1Bot @H2Bot String isBot3 = local3;
        @H1S1 @H2S1 String isSib13 = local3;
    }
}
