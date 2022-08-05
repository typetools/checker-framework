import org.checkerframework.checker.nullness.qual.*;

public class ConstructorPostcondition {

    class Box {
        @Nullable Object f;
    }

    @EnsuresNonNull("#1.f")
    // :: error: (contracts.postcondition.not.satisfied)
    ConstructorPostcondition(Box b) {}

    @EnsuresNonNull("#1.f")
    ConstructorPostcondition(Box b, Object o) {
        b.f = o;
    }

    void foo(Box b) {
        ConstructorPostcondition x = new ConstructorPostcondition(b, "x");
        b.f.hashCode();
    }
}
