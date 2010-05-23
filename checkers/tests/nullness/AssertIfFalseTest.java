import checkers.nullness.quals.*;

public class AssertIfFalseTest {

    @Nullable Object get() { return "m"; }

    @AssertNonNullIfFalse("get()")
    boolean isGettable() { return false; }

    void simple() {
        //:: (dereference.of.nullable)
        get().toString();
    }
    
    void checkWrongly() {
        if (isGettable()) {
            //:: (dereference.of.nullable)
            get().toString();
        }
    }

    void checkCorrectly() {
        if (!isGettable()) {
            get().toString();
        }
    }

    /** Returns whether or not constant_value is a legal constant **/
    @AssertNonNullIfFalse("#0")
    static boolean legalConstant (@Nullable Object constant_value) {
        return ((constant_value == null) || (constant_value instanceof Long)
                || (constant_value instanceof Double));
    }

    void useLegalConstant1 (@Nullable Object static_constant_value) {
        if (! legalConstant (static_constant_value)) {
            throw new AssertionError("unexpected constant class " + static_constant_value.getClass());
        }
    }
    void useLegalConstant2 (@Nullable Object static_constant_value) {
        assert legalConstant (static_constant_value)
            : "unexpected constant class " + static_constant_value.getClass();
    }

}
