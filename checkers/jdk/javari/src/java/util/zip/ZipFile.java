package java.util.zip;
import checkers.javari.quals.*;

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Enumeration;

public class ZipFile implements ZipConstants, Closeable {
    public static final int OPEN_READ;
    public static final int OPEN_DELETE;

    public ZipFile(String name) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public ZipFile(File file, int mode) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public ZipFile(File file) throws ZipException, IOException {
        throw new RuntimeException("skeleton method");
    }

    public ZipFile(File file, int mode, Charset charset) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public ZipFile(String name, Charset charset) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public ZipFile(File file, Charset charset) throws IOException {
        throw new RuntimeException("skeleton method");
    }
    
    public String getComment(@ReadOnly ZipFile this) {
        throw new RuntimeException("skeleton method");
    }

    public ZipEntry getEntry(String name) {
        throw new RuntimeException("skeleton method");
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public String getName(@ReadOnly ZipFile this) {
        throw new RuntimeException("skeleton method");
    }

    public Enumeration<? extends ZipEntry> entries() {
        throw new RuntimeException("skeleton method");
    }

    public int size(@ReadOnly ZipFile this) {
        throw new RuntimeException("skeleton method");
    }

    public void close() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    protected void finalize() throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
