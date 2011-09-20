package java.io;
import checkers.javari.quals.*;

public class BufferedInputStream extends FilterInputStream {
    public BufferedInputStream(InputStream in) {
        this(in, 0);
        throw new RuntimeException("skeleton method");
    }

    public BufferedInputStream(InputStream in, int size) {
        super(in);
        throw new RuntimeException("skeleton method");
    }

    public synchronized int read() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int read(byte b[], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized long skip(long n) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int available() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void mark(int readlimit) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void reset() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public boolean markSupported(@ReadOnly BufferedInputStream this) {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
