// Issue3105B is framework/src/test/java/testlib/lib/Issue3105B.java.
import static testlib.lib.Issue3105B.FIELD2;

import org.checkerframework.common.value.qual.StringVal;

class Issue3105UsesStaticImport {

    @StringVal("bar") String m2() {
        return FIELD2;
    }
}
