import checkers.util.test.*;
import java.util.*;

public abstract class MethodOverrideBadReturn {

    public abstract @Odd String method();

    public static class SubclassA extends MethodOverrideBadReturn {
        public String method() {
            return "";
        }
    }
}
