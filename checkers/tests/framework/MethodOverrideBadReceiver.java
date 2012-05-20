import tests.util.*;
import java.util.*;

public abstract class MethodOverrideBadReceiver {

    public abstract String method();

    public static class SubclassA extends MethodOverrideBadReceiver {
        //:: error: (override.receiver.invalid)
        public String method(@Odd SubclassA this) {
            return "";
        }
    }
}
