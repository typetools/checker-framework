// This is a test that the correct errors are thrown on this class, which is a copy of one defined
// by Zookeeper. Earlier versions of our code to handle this issued many duplicate
// inconsistent.mustcall.subtype
// errors. This test case doesn't actually test for that, but it's useful for debugging and as a
// regression
// test that at least one error is still issued.

import org.checkerframework.checker.mustcall.qual.MustCall;

import java.io.*;
import java.nio.ByteBuffer;

@MustCall({})
// :: error: inconsistent.mustcall.subtype
public class ZookeeperByteBufferInputStream extends InputStream {

    ByteBuffer bb;

    // :: error: super.invocation.invalid
    public ZookeeperByteBufferInputStream(ByteBuffer bb) {
        this.bb = bb;
    }

    @Override
    public int read() throws IOException {
        if (bb.remaining() == 0) {
            return -1;
        }
        return bb.get() & 0xff;
    }

    @Override
    public int available() throws IOException {
        return bb.remaining();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (bb.remaining() == 0) {
            return -1;
        }
        if (len > bb.remaining()) {
            len = bb.remaining();
        }
        bb.get(b, off, len);
        return len;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0L) {
            return 0;
        }
        n = Math.min(n, bb.remaining());
        bb.position(bb.position() + (int) n);
        return n;
    }
}
