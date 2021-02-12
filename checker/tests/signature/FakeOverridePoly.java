// @skip-test until fake overrides affect formal parameter names as well as return types

import javax.lang.model.element.Name;
import org.checkerframework.checker.signature.qual.CanonicalName;

public class FakeOverridePoly {

    void m(@CanonicalName Name n) {
        @CanonicalName String s = n.toString();
    }
}
