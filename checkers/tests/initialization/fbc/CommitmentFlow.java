import checkers.initialization.quals.*;
import checkers.nullness.quals.*;

public class CommitmentFlow {
    
    @NonNull CommitmentFlow t;
    
    public CommitmentFlow(CommitmentFlow arg) {
        t = arg;
    }
    
    void foo(@UnkownInitialization CommitmentFlow mystery, @Initialized CommitmentFlow triedAndTrue) {
        CommitmentFlow local = null;
        
        local = mystery;
        //:: error: (method.invocation.invalid)
        local.hashCode();
        
        local = triedAndTrue;
        local.hashCode(); // should determine that it is Committed based on flow
    }
}
