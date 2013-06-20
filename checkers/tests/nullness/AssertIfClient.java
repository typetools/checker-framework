import checkers.nullness.quals.*;

public class AssertIfClient {

  @NonNullOnEntry("#1.rpcResponse()")
  void rpcResponseNonNull(Proxy proxy) {
    // non-DFF branch issues a false alarm (problem parsing "#1. ...")
    @NonNull Object response = proxy.rpcResponse();
  }

  void rpcResponseNullable(Proxy proxy) {
    @Nullable Object response = proxy.rpcResponse();
  }
    
  void rpcResponseTypestate() {
    Proxy proxy = new Proxy();
    //:: error: (assignment.type.incompatible)
    @NonNull Object response1 = proxy.rpcResponse();
    // non-DFF branch suffers a missed alarm (problem parsing "#1. ...")
    //:: error: (argument.type.incompatible)
    rpcResponseNonNull(proxy);
    rpcResponseNullable(proxy);

    proxy.issueRpc();
    @NonNull Object response2 = proxy.rpcResponse();
    @NonNull Object response3 = proxy.rpcResponse();
    rpcResponseNonNull(proxy);
    rpcResponseNullable(proxy);
  }
    
}


class Proxy {

  // the RPC response, or null if not yet received
  @LazyNonNull Object response = null;

  @AssertNonNullAfter("rpcResponse()")
  void issueRpc() {
    response = new Object();
  }

  // If this method returns true,
  // then response is non-null and rpcResponse() returns non-null
  @AssertNonNullIfTrue("rpcResponse()")
  boolean rpcResponseReceived() {
    return response != null;
  }

  // Returns non-null if the response has been received, null otherwise;
  // but an @AssertNonNullIfNonNull annotation would states the converse,
  // that if the result is non-null then the response hs been received.
  // See rpcResponseReceived.
  @Pure @Nullable Object rpcResponse() {
    return response;
  }

}
