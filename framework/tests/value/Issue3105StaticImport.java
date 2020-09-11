// Issue3105Fields is framework/src/test/java/testlib/lib/Issue3105Fields.java.
import static testlib.lib.Issue3105Fields.FIELD2;

import org.checkerframework.common.value.qual.StringVal;

class Issue3105StaticImport {

    @StringVal("bar") String m2() {
        return FIELD2;
    }
}
