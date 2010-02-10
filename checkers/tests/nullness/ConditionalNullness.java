import checkers.nullness.quals.*;
import java.util.*;
public class ConditionalNullness {

    @AssertNonNullIfTrue({"field", "method()"})
    boolean checkNonNull() { return false; }

    @Nullable Object field = null;
    @Nullable Object method() { return "m"; }

    void testSelfWithCheck() {
        ConditionalNullness other = new ConditionalNullness();
        if (checkNonNull()) {
            field.toString();
            method().toString();
            //:: (dereference.of.nullable)
            other.field.toString(); // error
            //:: (dereference.of.nullable)
            other.method().toString();  // error
        }
        //:: (dereference.of.nullable)
        method().toString();   // error
    }

    void testSelfWithoutCheck() {
        //:: (dereference.of.nullable)
        field.toString();       // error
        //:: (dereference.of.nullable)
        method().toString();    // error
    }

    void testSelfWithCheckNegation() {
        if (checkNonNull()) { }
        else {
            //:: (dereference.of.nullable)
            field.toString();   // error
        }
        //:: (dereference.of.nullable)
        field.toString();       // error
    }

    void testOtherWithCheck() {
        ConditionalNullness other = new ConditionalNullness();
        if (other.checkNonNull()) {
            other.field.toString();
            other.method().toString();
            //:: (dereference.of.nullable)
            field.toString();   // error
            //:: (dereference.of.nullable)
            method().toString(); // error
        }
        //:: (dereference.of.nullable)
        other.method().toString();  // error
        //:: (dereference.of.nullable)
        method().toString();   // error
    }

    void testOtherWithoutCheck() {
        ConditionalNullness other = new ConditionalNullness();
        //:: (dereference.of.nullable)
        other.field.toString();     // error
        //:: (dereference.of.nullable)
        other.method().toString();  // error
        //:: (dereference.of.nullable)
        field.toString();       // error
        //:: (dereference.of.nullable)
        method().toString();    // error
    }

    void testOtherWithCheckNegation() {
        ConditionalNullness other = new ConditionalNullness();
        if (other.checkNonNull()) { }
        else {
            //:: (dereference.of.nullable)
            other.field.toString();     // error
            //:: (dereference.of.nullable)
            other.method().toString();  // error
            //:: (dereference.of.nullable)
            field.toString();   // error
        }
        //:: (dereference.of.nullable)
        field.toString();       // error
    }

}
