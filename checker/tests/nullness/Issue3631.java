import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class Issue3631 {

    void f(Object otherArg) {
        // Casts aren't a supported JavaExpression.
        // :: error: (contracts.precondition.not.satisfied)
        ((Issue3631Helper) otherArg).m();
    }
}

class Issue3631Helper {

    String type = "foo";

    @RequiresNonNull("type")
    void m() {}
}
