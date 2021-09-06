// A test case for https://github.com/typetools/checker-framework/issues/4838.

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

class ReplicaInputStreams implements Closeable {

    private final @Owning InputStream in1;
    private final @Owning InputStream in2;

    public ReplicaInputStreams(@Owning InputStream i1, @Owning InputStream i2) {
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
