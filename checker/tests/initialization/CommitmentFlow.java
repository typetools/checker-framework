import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CommitmentFlow {

  @NonNull CommitmentFlow t;

  public CommitmentFlow(CommitmentFlow arg) {
    t = arg;
  }

  void foo(
      @UnknownInitialization CommitmentFlow mystery, @Initialized CommitmentFlow triedAndTrue) {
    CommitmentFlow local = null;

    local = mystery;
    // :: error: (method.invocation.invalid)
    local.hashCode();

    local = triedAndTrue;
    local.hashCode(); // should determine that it is Initialized based on flow
  }
}
