import checkers.util.test.*;
import java.util.*;

public abstract class MethodOverrideBadReceiver {

    public abstract String method();

    public static class SubclassA extends MethodOverrideBadReceiver {
        public String method() @Odd {
            return "";
        }
    }
}
