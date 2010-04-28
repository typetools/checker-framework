import checkers.util.test.*;
import java.util.*;

public abstract class MethodOverrideBadReturn {

    public abstract @Odd String method();

    public static class SubclassA extends MethodOverrideBadReturn {
        //:: (override.return.invalid)
        public String method() {
            return "";
        }
    }
}
