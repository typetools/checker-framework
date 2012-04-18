package java.io;
import checkers.javari.quals.*;

import java.nio.channels.FileChannel;

public class FileInputStream extends InputStream {
    public FileInputStream(String name) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileInputStream(File file) throws FileNotFoundException {
        throw new RuntimeException("skeleton method");
    }

    public FileInputStream(FileDescriptor fdObj) {
        throw new RuntimeException("skeleton method");
    }

    public native int read() throws IOException;

    public int read(byte b[]) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public int read(byte b[], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public native long skip(long n) throws IOException;

    public native int available() throws IOException;

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final FileDescriptor getFD(@ReadOnly FileInputStream this) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public FileChannel getChannel() {
        throw new RuntimeException("skeleton method");
    }

    protected void finalize() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
