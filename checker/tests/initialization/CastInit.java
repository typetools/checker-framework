import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class CastInit {

    public CastInit() {
        @UnknownInitialization CastInit t1 = (@UnknownInitialization CastInit) this;
        // :: error: (initialization.invalid.cast)
        @Initialized CastInit t2 = (@Initialized CastInit) this;
    }
}
