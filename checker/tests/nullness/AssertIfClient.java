import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

public class AssertIfClient {

  @RequiresNonNull("#1.rpcResponse()")
  void rpcResponseNonNull(Proxy proxy) {
    @NonNull Object response = proxy.rpcResponse();
  }

  void rpcResponseNullable(Proxy proxy) {
    @Nullable Object response = proxy.rpcResponse();
  }

  void rpcResponseTypestate() {
    Proxy proxy = new Proxy();
    // :: error: (assignment.type.incompatible)
    @NonNull Object response1 = proxy.rpcResponse();
    // :: error: (contracts.precondition.not.satisfied)
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
  @MonotonicNonNull Object response = null;

  @SuppressWarnings("contracts.postcondition.not.satisfied")
  @EnsuresNonNull({"response", "rpcResponse()"})
  void issueRpc() {
    response = new Object();
  }

  // If this method returns true,
  // then response is non-null and rpcResponse() returns non-null
  @SuppressWarnings("contracts.conditional.postcondition.not.satisfied")
  @EnsuresNonNullIf(
      expression = {"response", "rpcResponse()"},
      result = true)
  boolean rpcResponseReceived() {
    return response != null;
  }

  // Returns non-null if the response has been received, null otherwise; but an
  // @AssertNonNullIfNonNull annotation would states the converse, that if the result is non-null
  // then the response hs been received.  See rpcResponseReceived.
  @Pure
  @Nullable Object rpcResponse() {
    return response;
  }
}
