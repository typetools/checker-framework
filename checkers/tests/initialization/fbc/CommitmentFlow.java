import checkers.initialization.quals.*;
import checkers.nonnull.quals.*;

public class CommitmentFlow {
    
    @NonNull CommitmentFlow t;
    
    public CommitmentFlow(CommitmentFlow arg) {
        t = arg;
    }
    
    void foo(@Unclassified CommitmentFlow mystery, @Committed CommitmentFlow triedAndTrue) {
        CommitmentFlow local = null;
        
        local = mystery;
        //:: error: (method.invocation.invalid)
        local.hashCode();
        
        local = triedAndTrue;
        local.hashCode(); // should determine that it is Committed based on flow
    }
}
