import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

import java.io.*;
import java.net.*;

class MultipleMethodParamsMustCallAliasTest {

    void testMultiMethodParamsCorrect1(@Owning InputStream in1, @Owning InputStream in2)
            throws IOException {

        ReplicaInputStreams r = new ReplicaInputStreams(in1, in2);

        r.close();
    }

    void testMultiMethodParamsCorrect2(@Owning InputStream in1, @Owning InputStream in2)
            throws IOException {

        ReplicaInputStreams r = new ReplicaInputStreams(in1, in2);

        try {
            in1.close();
        } catch (IOException e) {
        } finally {
            in2.close();
        }
    }

    void testMultiMethodParamsCorrect3(@Owning InputStream in1, @Owning InputStream in2)
            throws IOException {

        ReplicaInputStreams r = new ReplicaInputStreams(in1, in2);

        try {
            in1.close();
        } finally {
            in2.close();
        }
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

        private final @Owning InputStream in1;
        private final @Owning InputStream in2;

        public @MustCallAlias ReplicaInputStreams(
                @MustCallAlias InputStream i1, @MustCallAlias InputStream i2) {
            this.in1 = i1;
            this.in2 = i2;
        }

        @Override
        @EnsuresCalledMethods(
                value = {"this.in1", "this.in2"},
                methods = {"close"})
        // :: error: destructor.exceptional.postcondition
        public void close() throws IOException {
            in1.close();
            in2.close();
        }
    }
}
