
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class Cast {

    public Cast() {
        @UnknownInitialization Cast t1 = (@UnknownInitialization Cast) this;
        //:: error: (initialization.invalid.cast)
        @Initialized Cast t2 = (@Initialized Cast) this;
    }
}
