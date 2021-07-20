// Test for stub files and https://tinyurl.com/cfissue/658 .
// Commented in part because that issue is not yet fixed.

import org.checkerframework.checker.signature.qual.*;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;

public class PolySignatureTest2 {

    @CanonicalNameOrEmpty Name m1(TypeElement e) {
        return e.getQualifiedName();
    }

    @DotSeparatedIdentifiers String m2(@DotSeparatedIdentifiers Name n) {
        // :: error: (return.type.incompatible)
        return n.toString();
    }
}
