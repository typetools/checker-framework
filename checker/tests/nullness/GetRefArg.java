import org.checkerframework.checker.nullness.qual.*;

import java.lang.reflect.*;

public class GetRefArg {
    private void get_ref_arg(Constructor<?> constructor) throws Exception {
        Object val = constructor.newInstance();
        // :: warning: (nulltest.redundant)
        assert val != null;
    }
}
