import org.checkerframework.checker.signature.qual.*;

public class CanonicalNameNonEmptyTest {

    @CanonicalName String nonEmpty1(@CanonicalNameOrEmpty String s) {
        if (s.isEmpty()) {
            return null;
        } else {
            return s;
        }
    }

    @CanonicalName String nonEmpty2(@CanonicalNameOrEmpty String s) {
        if (!s.isEmpty()) {
            return s;
        } else {
            return null;
        }
    }

    @CanonicalName String nonEmpty3(@FullyQualifiedName String s) {
        if (s.isEmpty()) {
            return null;
        } else {
            // :: error: (return.type.incompatible)
            return s;
        }
    }
}
