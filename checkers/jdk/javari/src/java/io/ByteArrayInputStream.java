package java.io;
import checkers.javari.quals.*;

public class ByteArrayInputStream extends InputStream {
    protected byte buf[];
    protected int pos;
    protected int mark;
    protected int count;

    public ByteArrayInputStream(byte buf[]) {
        throw new RuntimeException("skeleton method");
    }

    public ByteArrayInputStream(byte buf[], int offset, int length) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int read() {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int read(byte b[], int off, int len) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized long skip(long n) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int available(@ReadOnly ByteArrayInputStream this) {
        throw new RuntimeException("skeleton method");
    }

    public boolean markSupported(@ReadOnly ByteArrayInputStream this) {
        throw new RuntimeException("skeleton method");
    }

    public void mark(int readAheadLimit) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void reset() {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
