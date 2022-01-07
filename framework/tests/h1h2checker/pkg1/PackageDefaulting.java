package pkg1;

import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S2;

/* Defaults from package pkg1 apply within the package. */
public class PackageDefaulting {
    // Test H1 hierarchy
    void m(@H1S1 @H2S1 Object p1, @H1S2 @H2S1 Object p2) {
        Object l1 = p1;
        // :: error: (assignment.type.incompatible)
        Object l2 = p2;
    }

    // Test H2 hierarchy
    void m2(@H1S1 @H2S1 Object p1, @H1S1 @H2S2 Object p2) {
        Object l1 = p1;
        // :: error: (assignment.type.incompatible)
        Object l2 = p2;
    }
}
