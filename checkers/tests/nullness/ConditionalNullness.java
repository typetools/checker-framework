import checkers.nullness.quals.*;
import java.util.*;
public class ConditionalNullness {

    @AssertNonNullIfTrue({"field", "method()"})
    boolean checkNonNull() {
        // don't bother with the implementation
        //:: error: (assertiftrue.postcondition.not.satisfied)
        return true;
    }

    @Nullable Object field = null;
    @Nullable Object method() { return "m"; }

    void testSelfWithCheck() {
        ConditionalNullness other = new ConditionalNullness();
        if (checkNonNull()) {
            field.toString();
            method().toString();
            //:: error: (dereference.of.nullable)
            other.field.toString(); // error
            //:: error: (dereference.of.nullable)
            other.method().toString();  // error
        }
        //:: error: (dereference.of.nullable)
        method().toString();   // error
    }

    void testSelfWithoutCheck() {
        //:: error: (dereference.of.nullable)
        field.toString();       // error
        //:: error: (dereference.of.nullable)
        method().toString();    // error
    }

    void testSelfWithCheckNegation() {
        if (checkNonNull()) { }
        else {
                //:: error: (dereference.of.nullable)
            field.toString();   // error
        }
        // TODO: actually, both branches ensure that field is non-null.
        // However, the NN checker does not recognize the NN in the
        // if branch, b/c it's implemented with a simple String pattern.
        //:: error: (dereference.of.nullable)
        field.toString();       // error
    }

    void testOtherWithCheck() {
        ConditionalNullness other = new ConditionalNullness();
        if (other.checkNonNull()) {
            other.field.toString();
            other.method().toString();
            //:: error: (dereference.of.nullable)
            field.toString();   // error
            //:: error: (dereference.of.nullable)
            method().toString(); // error
        }
        //:: error: (dereference.of.nullable)
        other.method().toString();  // error
        //:: error: (dereference.of.nullable)
        method().toString();   // error
    }

    void testOtherWithoutCheck() {
        ConditionalNullness other = new ConditionalNullness();
        //:: error: (dereference.of.nullable)
        other.field.toString();     // error
        //:: error: (dereference.of.nullable)
        other.method().toString();  // error
        //:: error: (dereference.of.nullable)
        field.toString();       // error
        //:: error: (dereference.of.nullable)
        method().toString();    // error
    }

    void testOtherWithCheckNegation() {
        ConditionalNullness other = new ConditionalNullness();
        if (other.checkNonNull()) { }
        else {
            //:: error: (dereference.of.nullable)
            other.field.toString();     // error
            //:: error: (dereference.of.nullable)
            other.method().toString();  // error
            //:: error: (dereference.of.nullable)
            field.toString();   // error
        }
        //:: error: (dereference.of.nullable)
        field.toString();       // error
    }

}
