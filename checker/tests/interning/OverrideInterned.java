import org.checkerframework.checker.interning.qual.Interned;

public class OverrideInterned {

  // This code is extracted from FreePastry

  @Interned class NodeHandle {}

  public interface TransportLayer<IDENTIFIER> {
    public void sendMessage(IDENTIFIER i);
  }

  public class CommonAPITransportLayerImpl<IDENTIFIER extends NodeHandle>
      implements TransportLayer<IDENTIFIER> {
    public void sendMessage(IDENTIFIER i) {}
  }

  interface MessageReceipt {
    public NodeHandle getHint();
  }

  void useAnonymousClass() {
    MessageReceipt ret =
        new MessageReceipt() {
          public NodeHandle getHint() {
            return null;
          }
        };
  }

  // This code is from Daikon

  public abstract class TwoSequenceString {
    public abstract Object check_modified1(@Interned String @Interned [] v1);

    public abstract Object check_modified2(String @Interned [] v1);
  }

  /* Changing the array component type in the overriding method is illegal. */
  public class PairwiseStringEqualBad extends TwoSequenceString {
    // TODOINVARR:: error: (override.param.invalid)
    public Object check_modified1(String @Interned [] a1) {
      return new Object();
    }
    // :: error: (override.param.invalid)
    public Object check_modified2(@Interned String @Interned [] a1) {
      return new Object();
    }
  }

  /* Changing the main reference type is allowed, if it is a supertype. */
  public class PairwiseStringEqualGood extends TwoSequenceString {
    public Object check_modified1(@Interned String[] a1) {
      return new Object();
    }

    public Object check_modified2(String[] a1) {
      return new Object();
    }
  }
}
