
import checkers.initialization.quals.*;

class Cast {
    
    public Cast() {
        @UnkownInitialization Cast t1 = (@UnkownInitialization Cast) this;
        //:: error: (commitment.invalid.cast)
        @Initialized Cast t2 = (@Initialized Cast) this;
    }
    
}
