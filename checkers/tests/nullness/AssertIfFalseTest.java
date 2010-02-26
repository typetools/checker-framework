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
}
