import checkers.util.test.*;
import java.util.*;

public abstract class MethodOverrides {

    public abstract @Odd String method();
    public abstract String methodSub();

    public abstract void param(@Odd String s);
    public abstract void paramSup(@Odd String s);

    public abstract void receiver() @Odd;
    public abstract void receiverSub() @Odd;

    public static class SubclassA extends MethodOverrides {

        public @Odd String method() {
            return (@Odd String)"";
        }

        public @Odd String methodSub() {
            return (@Odd String)"";
        }

        public void param(@Odd String s) {}
        public void paramSup(String s) {}

        public void receiver() @Odd {}
        public void receiverSub() {}
    }

    static class X {
        <T> T @Odd [] method(T @Odd [] t) {
            return null;
        }
    }

    static class Y extends X {
        @Override <T> T @Odd [] method(T @Odd [] t) {
            return null;
        }
    }

    // TODO others...
}
