package java.io;

import checkers.javari.quals.*;

public interface DataInput {
    public boolean readBoolean() throws IOException;
    public byte readByte() throws IOException;
    public char readChar() throws IOException;
    public double readDouble() throws IOException;
    public float readFloat() throws IOException;
    public void readFully(byte[] b) throws IOException;
    public void readFully(byte[] b, int off, int len) throws IOException;
    public int readInt() throws IOException;
    public String readLine() throws IOException;
    public long readLong() throws IOException;
    public short readShort() throws IOException;
    public int readUnsignedByte() throws IOException;
    public int readUnsignedShort() throws IOException;
    public String readUTF() throws IOException;
    public int skipBytes(int n) throws IOException;
}
