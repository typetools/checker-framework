// Issue3105Fields is
// framework/src/test/java/org/checkerframework/framework/testchecker/lib/Issue3105Fields.java.
import static org.checkerframework.framework.testchecker.lib.Issue3105Fields.FIELD2;

import org.checkerframework.common.value.qual.StringVal;

public class Issue3105StaticImport {

  @StringVal("bar") String m2() {
    return FIELD2;
  }
}
