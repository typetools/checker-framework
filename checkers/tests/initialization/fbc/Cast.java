
import checkers.initialization.quals.*;

class Cast {
    
    public Cast() {
        @UnknownInitialization Cast t1 = (@UnknownInitialization Cast) this;
        //:: error: (commitment.invalid.cast)
        @Initialized Cast t2 = (@Initialized Cast) this;
    }
    
}
