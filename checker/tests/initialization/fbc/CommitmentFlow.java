import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class CommitmentFlow {

    @NonNull CommitmentFlow t;

    public CommitmentFlow(CommitmentFlow arg) {
        t = arg;
    }

    void foo(@UnknownInitialization CommitmentFlow mystery, @Initialized CommitmentFlow triedAndTrue) {
        CommitmentFlow local = null;

        local = mystery;
        //:: error: (method.invocation.invalid)
        local.hashCode();

        local = triedAndTrue;
        local.hashCode(); // should determine that it is Committed based on flow
    }
}
