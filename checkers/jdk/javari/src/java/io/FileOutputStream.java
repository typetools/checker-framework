package java.io;
import checkers.javari.quals.*;

import java.nio.channels.FileChannel;
import sun.nio.ch.FileChannelImpl;

public class FileOutputStream extends OutputStream {
    private final FileDescriptor fd;

    private FileChannel channel= null;

    private final Object closeLock = new Object();
    private volatile boolean closed = false;
    private static final ThreadLocal<Boolean> runningFinalize =
        new ThreadLocal<Boolean>();

    public FileOutputStream(String name) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileOutputStream(String name, boolean append) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileOutputStream(File file) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileOutputStream(File file, boolean append) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileOutputStream(FileDescriptor fdObj) {
        throw new RuntimeException("skeleton method");
    }

    public native void write(int b) throws IOException;

    private native void writeBytes(byte b[], int off, int len) throws IOException;

    public void write(byte b @ReadOnly []) throws IOException {
        writeBytes(b, 0, b.length);
    }

    public void write(byte b @ReadOnly [], int off, int len) throws IOException {
        writeBytes(b, off, len);
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final FileDescriptor getFD() @ReadOnly throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public FileChannel getChannel() {
        throw new RuntimeException("skeleton method");
    }

    protected void finalize() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
