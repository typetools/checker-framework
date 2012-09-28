
import java.lang.reflect.*;
import checkers.nonnull.quals.*;

public class GetRefArg {
    private void get_ref_arg (Constructor<?> constructor) throws Exception {
        Object val = constructor.newInstance(null);
        assert val != null;
    }
}
