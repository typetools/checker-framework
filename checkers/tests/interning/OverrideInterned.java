import checkers.interning.quals.Interned;

// This code is extracted from FreePastry
class OverrideInterned {

  @Interned class NodeHandle { }

  public interface TransportLayer<IDENTIFIER> {
    public void sendMessage(IDENTIFIER i);
  }

  public class CommonAPITransportLayerImpl<IDENTIFIER extends NodeHandle>
    implements TransportLayer<IDENTIFIER>
  {
    public void sendMessage(IDENTIFIER i) { }
  }

  interface MessageReceipt {
    public NodeHandle getHint();
  }

  void useAnonymousClass() {
    MessageReceipt ret = new MessageReceipt(){
      public NodeHandle getHint() {
        return null;
      }
    };
  }

}
