package java.io;
import checkers.javari.quals.*;

public class BufferedInputStream extends FilterInputStream {
    private static int defaultBufferSize = 8192;
    protected volatile byte buf[];
    protected int count;
    protected int pos;
    protected int markpos = -1;
    protected int marklimit;

    public BufferedInputStream(InputStream in) {
        this(in, defaultBufferSize);  // for some reason removing this breaks compilation
    }

    public BufferedInputStream(InputStream in, int size) {
        super(in); // for some reason removing this breaks compilation
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

    public boolean markSupported() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
