
import java.lang.reflect.*;

class OptionInfo {
    /*@Nullable*/ Constructor<?> constructor = null;
}

public class GetRefArg {
    private /*@NonNull*/ Object get_ref_arg (OptionInfo oi) throws Exception {
        Object val = oi.constructor.newInstance (null);

        assert val != null : "@SuppressWarnings(nullness)";
        return val;
    }
}
