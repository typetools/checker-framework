import java.lang.Thread.State;
import org.checkerframework.checker.tainting.qual.*;

class EnumStubTest {
    void test() {
        requireEnum(State.NEW);
        // :: error: (argument.type.incompatible)
        requireEnum(State.RUNNABLE);
    }

    void requireEnum(@Untainted State sEnum) {}
}
