// @skip-test until fake overrides affect formal parameter types as well as return types

import org.checkerframework.checker.signature.qual.CanonicalName;

import javax.lang.model.element.Name;

public class FakeOverridePoly {

    void m(@CanonicalName Name n) {
        @CanonicalName String s = n.toString();
    }
}
