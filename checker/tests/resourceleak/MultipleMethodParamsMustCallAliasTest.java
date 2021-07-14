import java.io.*;
import java.net.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class MultipleMethodParamsMustCallAliasTest {

  void testMultiMethodParamsCorrect(@Owning InputStream in1, @Owning InputStream in2)
      throws IOException {

    ReplicaInputStreams r = new ReplicaInputStreams(in1, in2);

    r.close();
  }

  // :: error: required.method.not.called
  void testMultiMethodParamsWrong1(@Owning InputStream in1, @Owning InputStream in2)
      throws IOException {

    ReplicaInputStreams r = new ReplicaInputStreams(in1, in2);

    in1.close();
  }

  // :: error: required.method.not.called
  void testMultiMethodParamsWrong2(@Owning InputStream in1, @Owning InputStream in2)
      throws IOException {

    ReplicaInputStreams r = new ReplicaInputStreams(in1, in2);

    in2.close();
  }

  // :: error: required.method.not.called
  void testMultiMethodParamsWrong3(@Owning InputStream in1) throws IOException {
    // :: error: required.method.not.called
    Socket socket = new Socket("address", 12);
    ReplicaInputStreams r = new ReplicaInputStreams(in1, socket.getInputStream());
  }

  class ReplicaInputStreams implements Closeable {

    private final @Owning InputStream out1;
    private final @Owning InputStream out2;

    public @MustCallAlias ReplicaInputStreams(
        @MustCallAlias InputStream o1, @MustCallAlias InputStream o2) {
      this.out1 = o1;
      this.out2 = o2;
    }

    @Override
    @EnsuresCalledMethods(
        value = {"this.out1", "this.out2"},
        methods = {"close"})
    public void close() throws IOException {
      out1.close();
      out2.close();
    }
  }
}
