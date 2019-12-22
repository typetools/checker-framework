// warning: StubParser: Method thisMethodIsNotReal(String) not found in type java.lang.String
// warning: StubParser: Type not found: java.lang.NotARealClass
// warning: StubParser: Type not found: not.real.NotARealClassInNotRealPackage

import org.checkerframework.checker.nullness.qual.*;

public class TestEnsuresNonNullIfStub {

    @Nullable String s = null;

    @Nullable StringBuffer sb = null;

    @Nullable CharSequence cs = null;

    void method(String arg) {
        if (arg.startsWith(s)) {
            @NonNull String s2 = s;
        }

        if (arg.endsWith(s)) {
            @NonNull String s3 = s;
        }
    }
}
