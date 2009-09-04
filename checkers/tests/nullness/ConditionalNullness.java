import checkers.nullness.quals.*;

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
            other.field.toString(); // error
            other.method().toString();  // error
        }
        method().toString();   // error
    }

    void testSelfWithoutCheck() {
        field.toString();       // error
        method().toString();    // error
    }

    void testSelfWithCheckNegation() {
        if (checkNonNull()) { }
        else {
            field.toString();   // error
        }
        field.toString();       // error
    }

    void testOtherWithCheck() {
        ConditionalNullness other = new ConditionalNullness();
        if (other.checkNonNull()) {
            other.field.toString();
            other.method().toString();
            field.toString();   // error
            method().toString(); // error
        }
        other.method().toString();  // error
        method().toString();   // error
    }

    void testOtherWithoutCheck() {
        ConditionalNullness other = new ConditionalNullness();
        other.field.toString();     // error
        other.method().toString();  // error
        field.toString();       // error
        method().toString();    // error
    }

    void testOtherWithCheckNegation() {
        ConditionalNullness other = new ConditionalNullness();
        if (other.checkNonNull()) { }
        else {
            other.field.toString();     // error
            other.method().toString();  // error
            field.toString();   // error
        }
        field.toString();       // error
    }

}
