package java.io;
import checkers.javari.quals.*;

public class BufferedOutputStream extends FilterOutputStream {
    protected byte buf[];
    protected int count;

    public BufferedOutputStream(OutputStream out) {
        throw new RuntimeException("skeleton method");
    }

    public BufferedOutputStream(OutputStream out, int size) {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void write(int b) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void write(byte b @ReadOnly [], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public synchronized void flush() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
