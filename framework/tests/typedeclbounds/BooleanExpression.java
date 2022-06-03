import org.checkerframework.framework.testchecker.typedeclbounds.quals.*;

// Test the @S2 upperbound for boolean expression
class BooleanExpression {

    boolean equal;
    boolean notEqual;
    boolean lessThan;
    boolean greaterThan;
    boolean lessThanEqual;
    boolean greaterThanEqual;

    // ensures @Top is not within the bounds for boolean
    // :: error: (type.invalid.annotations.on.use)
    @Top boolean topBoolean;

    // ensures @S1 is not within the bounds for boolean
    // :: error: (type.invalid.annotations.on.use)
    @S1 boolean s1Boolean;

    @Bottom boolean bottomBoolean;

    // ensures the @S2 upperbound is applied to binary comparison
    void compareTop(@Top Integer x, @Top Integer y) {
        equal = x == y;
        notEqual = x != y;
        lessThan = x < y;
        greaterThan = x > y;
        lessThanEqual = x <= y;
        greaterThanEqual = x >= y;
    }

    // ensures the @S2 upperbound is applied to binary comparison
    void compareBottom(@Bottom Integer x, @Bottom Integer y) {
        equal = x == y;
        notEqual = x != y;
        lessThan = x < y;
        greaterThan = x > y;
        lessThanEqual = x <= y;
        greaterThanEqual = x >= y;
    }

    // ensures the default type is not @Bottom
    void assignBottom() {
        // :: error: (assignment.type.incompatible)
        bottomBoolean = equal;
    }

    // ensures the @S2 upperbound is applied to instanceof
    static @S2 boolean isNumber(@Top Object obj) {
        return obj instanceof Number;
    }
}
