
import org.checkerframework.checker.initialization.qual.*;

class Cast {

    public Cast() {
        @UnknownInitialization Cast t1 = (@UnknownInitialization Cast) this;
        //:: error: (initialization.invalid.cast)
        @Initialized Cast t2 = (@Initialized Cast) this;
    }

}
