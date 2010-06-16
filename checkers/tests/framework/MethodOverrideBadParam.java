import checkers.util.test.*;
import java.util.*;

public abstract class MethodOverrideBadParam {

    public abstract void method(String s);

    public static class SubclassA extends MethodOverrideBadParam {
        //:: (override.param.invalid)
        public void method(@Odd String s) {

        }
    }
}
