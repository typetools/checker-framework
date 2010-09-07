package java.io;
import checkers.javari.quals.*;

public class DataInputStream extends FilterInputStream implements DataInput {

    public DataInputStream(InputStream in) {
        super(in); // removing this breaks compilation for some reason
        throw new RuntimeException("skeleton method");
    }

    public final int read(byte b[]) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final int read(byte b[], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final void readFully(byte b[]) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final void readFully(byte b[], int off, int len) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final int skipBytes(int n) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final boolean readBoolean() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final byte readByte() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final int readUnsignedByte() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final short readShort() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final int readUnsignedShort() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final char readChar() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final int readInt() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final long readLong() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final float readFloat() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final double readDouble() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    @Deprecated
    public final String readLine() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final String readUTF() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public final static String readUTF(DataInput in) throws IOException {
        throw new RuntimeException("skeleton method");
    }
}
